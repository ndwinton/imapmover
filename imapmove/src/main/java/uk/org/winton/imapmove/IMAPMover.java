package uk.org.winton.imapmove;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.mail.Address;
import javax.mail.Flags;
import javax.mail.Flags.Flag;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.log4j.Logger;

public class IMAPMover {
	private static final Logger LOG = Logger.getLogger(IMAPMover.class);
	
	private IMAPClient source;
	private IMAPClient destination;
	private String subjectPrefix;
	
	public IMAPMover(IMAPClient src, IMAPClient dst) {
		this.source = src;
		this.destination = dst;
	}

	public void move() throws MessagingException {
		move(true);
	}
	
	public void move(boolean expunge) throws MessagingException {
		LOG.info("Starting message move" );
		Folder src = source.getMailboxFolder();
		Folder dst = destination.getMailboxFolder();
		
		src.open(Folder.READ_WRITE);
		Message[] msgs = src.getMessages();
		LOG.info(msgs.length + " message(s) found in source mailbox");
		Message[] processed = processSourceMessages(msgs);
		LOG.info(processed.length + " message(s) will be moved");
		
		dst.open(Folder.READ_WRITE);
		src.copyMessages(processed, dst);
		LOG.info("Messages moved successfully");
		
		markNonSkippedMessagesForDeletion(msgs);
		LOG.info("Original messages marked for deletion");
		
		if (expunge) {
			src.expunge();
		}
	}

	private Message[] processSourceMessages(Message[] original) throws MessagingException {
		List<Message> processed = new ArrayList<Message>();
		
		for (int i = 0; i < original.length; i++) {
			MimeMessage srcMime = (MimeMessage)original[i];
			MimeMessage dstMime = new MimeMessage(srcMime);
			
			String from = "UNKNOWN";
			if (srcMime.getFrom() != null) {
				from = srcMime.getFrom()[0].toString();
			}
			LOG.info("Message: " + srcMime.getSubject() + " (" + from + ")");
			
			if (messageShouldBeSkipped(srcMime, true)) {
				continue;
			}

			// Update the Subject line, if necessary
			dstMime.setSubject(getSubjectPrefix() + dstMime.getSubject());
			
			// Sanitize the addresses
			replaceRecipient(Message.RecipientType.TO, dstMime);
			replaceRecipient(Message.RecipientType.CC, dstMime);
			replaceRecipient(Message.RecipientType.BCC, dstMime);
			
			// Clear any flags
			dstMime.setFlags(dstMime.getFlags(), false);
			
			processed.add(dstMime);

		}
		return processed.toArray(new Message[processed.size()]);
	}

	private boolean messageShouldBeSkipped(MimeMessage msg, boolean doLog) throws MessagingException {
		if (messageIsFromDestination(msg)) {
			if (doLog) {
				LOG.info("From destination -- skipped");
			}
			return true;
		}
		
		if (msg.getFlags().contains(Flag.DELETED)) {
			if (doLog) {
				LOG.info("Already deleted -- skipped");
			}
			return true;
		}
		
		return false;
	}
	
	private boolean messageIsFromDestination(MimeMessage msg) throws MessagingException {
		InternetAddress[] fromAddrs = (InternetAddress[])msg.getFrom();
		if (fromAddrs != null) {
			boolean fromDestination = false;
			for (InternetAddress from : fromAddrs) {
				if (destination.getEmailAddress().equals(from.getAddress())) {
					fromDestination = true;
				}
			}
			if (fromDestination) {
				return true;
			}
		}
		return false;
	}
	
	private void replaceRecipient(RecipientType type, MimeMessage mime) throws MessagingException, AddressException {
		Address[] toAddrs = mime.getRecipients(type);
		if (toAddrs == null) {
			return;
		}
		
		for (int i = 0; i < toAddrs.length; i++) {
			InternetAddress addr = (InternetAddress)toAddrs[i];
			if (source.getEmailAddress().equals(addr.getAddress())) {
				InternetAddress newTo = new InternetAddress(destination.getEmailAddress());
				toAddrs[i] = newTo;
			}
		}
		mime.setRecipients(type, toAddrs);
	}
	
	private void markNonSkippedMessagesForDeletion(Message[] original) throws MessagingException {
		
		for (int i = 0; i < original.length; i++) {
			MimeMessage srcMime = (MimeMessage)original[i];
			
			if (!messageShouldBeSkipped(srcMime, false)) {
				srcMime.setFlag(Flags.Flag.DELETED, true);
			}
		}
	}

	public IMAPClient getSource() {
		return source;
	}

	public void setSource(IMAPClient srcClient) {
		this.source = srcClient;
	}

	public IMAPClient getDestination() {
		return destination;
	}

	public void setDestination(IMAPClient dstClient) {
		this.destination = dstClient;
	}

	public void setSubjectPrefix(String prefix) {
		subjectPrefix = prefix;
	}

	public String getSubjectPrefix() {
		return subjectPrefix == null ? "" : subjectPrefix;
	}

	public static void main(String[] args) throws MessagingException, FileNotFoundException, IOException {
		IMAPMover mover = null;
		
		if (args.length == 3) {
			mover = new IMAPMover(new IMAPClient(args[0]), new IMAPClient(args[1]));
			mover.setSubjectPrefix(args[2]);
		}
		else if (args.length == 2) {
			IMAPClient src = new IMAPClient();
			src.initialiseFromProperties(new FileInputStream(new File(args[0])), "source.");
			IMAPClient dest = new IMAPClient();
			dest.initialiseFromProperties(new FileInputStream(new File(args[0])), "destination.");
			mover = new IMAPMover(src, dest);
			mover.setSubjectPrefix(args[1]);
		}
		else {
			System.err.println("Usage: IMAPMover src-imap-url dest-imap-url prefix");
			System.err.println("  or   IMAPMover properties-file prefix");
			System.exit(1);
		}
		
		mover.move(true);
	}

}
