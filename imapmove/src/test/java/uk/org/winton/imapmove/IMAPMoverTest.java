package uk.org.winton.imapmove;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import javax.mail.Address;
import javax.mail.Flags.Flag;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.MimeMessage;

import org.junit.Before;
import org.junit.Test;
import org.jvnet.mock_javamail.Mailbox;

public class IMAPMoverTest {
	private static final String DSTUSER = "dstuser@localhost";
	private static final String SRCUSER = "srcuser@localhost";
	private static final int DST_MSG_COUNT = 3;
	private static final int SRC_MSG_COUNT = 6;
	private static final String SRC_URL = "imap://srcuser@localhost/inbox";
	private static final String DST_URL = "imap://dstuser@localhost/inbox";
	private Mailbox srcMbx;
	private Mailbox dstMbx;
	private IMAPMover mover;
	
	@Before
	public void setUp() throws Exception {
		Mailbox.clearAll();
		srcMbx = Mailbox.get(SRCUSER);
		dstMbx = Mailbox.get(DSTUSER);
		List<Message> srcMsgs = new ArrayList<Message>();
		List<Message> dstMsgs = new ArrayList<Message>();
		Session session = Session.getInstance(System.getProperties());
		
		for (int i = 1; i <= SRC_MSG_COUNT; i++) {
			MimeMessage msg = new MimeMessage(session);
			msg.setRecipients(Message.RecipientType.TO, SRCUSER + ", to" + i + "@localhost");
			msg.setRecipients(Message.RecipientType.CC, "cc" + i + "@localhost, " + SRCUSER);
			if (i % 2 == 0) {
				msg.setRecipients(Message.RecipientType.BCC, "bcc" + i + "@localhost, " + SRCUSER);
			}
			msg.setSubject("Source Subject " + i);
			msg.setText("Some body text");
			srcMsgs.add(msg);
		}
		srcMbx.addAll(srcMsgs);
	
		for (int i = 1; i <= DST_MSG_COUNT; i++) {
			MimeMessage msg = new MimeMessage(session);
			msg.setRecipients(Message.RecipientType.TO, DSTUSER + ", to" + i + "@localhost");
			msg.setRecipients(Message.RecipientType.CC, "cc" + i + "@localhost");
			msg.setSubject("Destination Subject " + i);
			msg.setText("Some body text");
			dstMsgs.add(msg);
		}
		dstMbx.addAll(dstMsgs);
		
		mover = new IMAPMover(new IMAPClient(SRC_URL), new IMAPClient(DST_URL));
	}

	@Test
	public void shouldBeAbleToConstructMoverWithTwoIMAPClients() {
		
		IMAPMover mover = new IMAPMover(new IMAPClient(SRC_URL), new IMAPClient(DST_URL));
		assertNotNull(mover);
	}
	
	@Test
	public void messagesShouldBeAddedToDestinationMailboxAndRemovedFromSource() throws MessagingException {
		assertEquals(SRC_MSG_COUNT, srcMbx.size());
		assertEquals(DST_MSG_COUNT, dstMbx.size());
	
		mover.move();
		
		assertEquals(SRC_MSG_COUNT + DST_MSG_COUNT, dstMbx.size());
		assertEquals(0, srcMbx.size());
	}

	@Test
	public void shouldBePossibleToSpecifyAMovedMessageSubjectPrefix() {
		mover.setSubjectPrefix("[Moved]");
		assertEquals("[Moved]", mover.getSubjectPrefix());
	}
	
	@Test
	public void subjectPrefixShouldBePrependedToMovedMessageSubjectIfSet() throws MessagingException {
		mover.setSubjectPrefix("MOVED ");
		mover.move();
		int matched = 0;
		for (Message msg : dstMbx) {
			String subject = msg.getSubject();
			if (subject.startsWith("MOVED ")) {
				matched++;
			}
		}
		assertEquals(SRC_MSG_COUNT, matched);
	}
	
	@Test
	public void subjectShouldBeUntouchedIfNoPrefixSet() throws MessagingException {
		mover.setSubjectPrefix(null);
		mover.move();
		int matched = 0;
		for (Message msg : dstMbx) {
			String subject = msg.getSubject();
			if (subject.startsWith("Source Subject")) {
				matched++;
			}
		}
		assertEquals(SRC_MSG_COUNT, matched);
	}
	
	@Test
	public void originalRecipientShouldBeReplacedInToLine() throws MessagingException {
		mover.move();
		int matched = 0;
		for (Message msg : dstMbx) {
			Address[] addrs = msg.getRecipients(Message.RecipientType.TO);
			for (Address addr : addrs) {
				String sad = addr.toString();
				if (sad.contains(DSTUSER)) {
					matched++;
				}
			}
		}
		assertEquals(SRC_MSG_COUNT + DST_MSG_COUNT, matched);
	}

	@Test
	public void originalRecipientShouldBeReplacedInCcLine() throws MessagingException {
		mover.move();
		int matched = 0;
		for (Message msg : dstMbx) {
			Address[] addrs = msg.getRecipients(Message.RecipientType.CC);
			for (Address addr : addrs) {
				String sad = addr.toString();
				if (sad.contains(DSTUSER)) {
					matched++;
				}
			}
		}
		assertEquals(SRC_MSG_COUNT, matched);
	}

	@Test
	public void originalRecipientShouldBeReplacedInBccLine() throws MessagingException {
		mover.move();
		int matched = 0;
		for (Message msg : dstMbx) {
			Address[] addrs = msg.getRecipients(Message.RecipientType.BCC);
			if (addrs != null) {
				for (Address addr : addrs) {
					String sad = addr.toString();
					if (sad.contains(DSTUSER)) {
						matched++;
					}
				}
			}
		}
		assertEquals(SRC_MSG_COUNT / 2, matched);
	}
	
	@Test
	public void messagesFromDestinationAddressShouldNotBeMoved() throws MessagingException {
		srcMbx.clear();
		dstMbx.clear();
		List<Message> srcMsgs = new ArrayList<Message>();
		Session session = Session.getInstance(System.getProperties());
		
		for (int i = 1; i <= SRC_MSG_COUNT; i++) {
			MimeMessage msg = new MimeMessage(session);
			msg.setRecipients(Message.RecipientType.TO, SRCUSER + ", to" + i + "@localhost");
			if (i % 2 == 0) {
				// Two variations of address -- with or without full name present
				if (i % 3 == 0) {
					msg.setFrom(DSTUSER);
				}
				else {
					msg.setFrom("Destination User <" + DSTUSER + ">");
				}
			}
			msg.setSubject("Source Subject " + i);
			msg.setText("Some body text");
			srcMsgs.add(msg);
		}
		srcMbx.addAll(srcMsgs);
		
		assertEquals(SRC_MSG_COUNT, srcMbx.size());
		assertEquals(0, dstMbx.size());
		
		mover.move();
		
		assertEquals(SRC_MSG_COUNT / 2, srcMbx.size());
		assertEquals(SRC_MSG_COUNT / 2, dstMbx.size());
	}
	
	@Test
	public void messagesShouldBeAddedToDestinationButNotRemovedFromSourceIfExpungeFlagIsFalse() throws MessagingException {
		assertEquals(SRC_MSG_COUNT, srcMbx.size());
		assertEquals(DST_MSG_COUNT, dstMbx.size());
	
		mover.move(false);
		
		assertEquals(SRC_MSG_COUNT + DST_MSG_COUNT, dstMbx.size());
		assertEquals(SRC_MSG_COUNT, srcMbx.size());
	}
	
	@Test
	public void deletedMessagesShouldNotBeMoved() throws MessagingException {
		srcMbx.clear();
		dstMbx.clear();
		List<Message> srcMsgs = new ArrayList<Message>();
		Session session = Session.getInstance(System.getProperties());
		
		for (int i = 1; i <= SRC_MSG_COUNT; i++) {
			MimeMessage msg = new MimeMessage(session);
			msg.setFrom("from@somewhere");
			msg.setRecipients(Message.RecipientType.TO, SRCUSER + ", to" + i + "@localhost");
			if (i % 2 == 0) {
				msg.setFlag(Flag.DELETED, true);
			}
			msg.setSubject("Source Subject " + i);
			msg.setText("Some body text");
			srcMsgs.add(msg);
		}
		srcMbx.addAll(srcMsgs);
		
		assertEquals(SRC_MSG_COUNT, srcMbx.size());
		assertEquals(0, dstMbx.size());
		
		mover.move();
		
		assertEquals(0, srcMbx.size());
		assertEquals(SRC_MSG_COUNT / 2, dstMbx.size());
	}

	@Test
	public void messageFlagsShouldBeClearedFromSourceMessages() throws MessagingException {
		srcMbx.clear();
		dstMbx.clear();
		List<Message> srcMsgs = new ArrayList<Message>();
		Session session = Session.getInstance(System.getProperties());
		
		for (int i = 1; i <= SRC_MSG_COUNT; i++) {
			MimeMessage msg = new MimeMessage(session);
			msg.setFrom("from@somewhere");
			msg.setRecipients(Message.RecipientType.TO, SRCUSER + ", to" + i + "@localhost");
			if (i % 2 == 0) {
				msg.setFlag(Flag.FLAGGED, true);
				msg.setFlag(Flag.ANSWERED, true);
			}
			msg.setSubject("Source Subject " + i);
			msg.setText("Some body text");
			srcMsgs.add(msg);
		}
		srcMbx.addAll(srcMsgs);
		
		assertEquals(SRC_MSG_COUNT, srcMbx.size());
		assertEquals(0, dstMbx.size());
		
		mover.move();
		
		assertEquals(0, srcMbx.size());
		assertEquals(SRC_MSG_COUNT, dstMbx.size());
		for (Message msg : dstMbx) {
			assertFalse(msg.getFlags().contains(Flag.ANSWERED));
			assertFalse(msg.getFlags().contains(Flag.FLAGGED));
		}
	}
}
