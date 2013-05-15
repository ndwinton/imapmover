package uk.org.winton.imapmove;

import java.util.Properties;

import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.URLName;

public class IMAPClient {

	static final String MAIL_DEBUG = "mail.debug";
	static final String MAIL_FROM = "mail.from";
	static final String MAIL_IMAP_SSL_ENABLE = "mail.imap.ssl.enable";
	static final String DEFAULT_MAILBOX = "INBOX";
	static final String MAIL_USER = "mail.user";
	static final String MAIL_PASSWORD = "mail.password";
	static final String MAIL_HOST = "mail.host";
	static final String MAIL_PORT = "mail.port";
	static final String MAIL_STORE_PROTOCOL = "mail.store.protocol";
	static final int DEFAULT_LDAP_PORT = 143;
	static final int DEFAULT_LDAPS_PORT = 993;

	private Properties properties;
	private String mailbox;
	private Session session;
	private Store store;
	private boolean debugEnabled;
		
	public IMAPClient(String url) {
		this(new URLName(url));
	}
	
	public IMAPClient(URLName urlName) {
		properties = new Properties();
		setProtocol(urlName.getProtocol());
		setUsername(urlName.getUsername());
		setPassword(urlName.getPassword());
		setHost(urlName.getHost());
		setPort(urlName.getPort());
		setMailbox(urlName.getFile());
		setDebug(false);
		setEmailAddress(null);
	}


	public void setUsername(String username) {
		if (username == null) {
			properties.remove(MAIL_USER);
		}
		else {
			properties.put(MAIL_USER, username);
		}
	}

	public String getUsername() {
		return properties.getProperty(MAIL_USER);
	}

	public void setPassword(String password) {
		if (password == null) {
			properties.remove(MAIL_PASSWORD);
		}
		else {
			properties.put(MAIL_PASSWORD, password);
		}
	}

	public String getPassword() {
		return properties.getProperty(MAIL_PASSWORD);
	}

	public void setHost(String host) {
		properties.put(MAIL_HOST, (host == null ? "localhost" : host));	
	}
	
	public String getHost() {
		return properties.getProperty(MAIL_HOST);
	}

	public void setPort(int port) {
		if (port <= 0) {
			port = isSSL() ? DEFAULT_LDAPS_PORT : DEFAULT_LDAP_PORT;
		}
		properties.put(MAIL_PORT, Integer.toString(port));
	}
	
	public int getPort() {
		return Integer.parseInt(properties.getProperty(MAIL_PORT));
	}

	public void setMailbox(String mailbox) {
		this.mailbox = mailbox;
	}
	
	public String getMailbox() {
		return mailbox == null ? DEFAULT_MAILBOX : mailbox;
	}

	public void setProtocol(String protocol) {
		if (protocol == null) {
			protocol = "imap";
		}
		properties.put(MAIL_STORE_PROTOCOL, protocol);
		properties.put(MAIL_IMAP_SSL_ENABLE, (isSSL() ? "true" : "false"));
	}
	
	public String getProtocol() {
		return properties.getProperty(MAIL_STORE_PROTOCOL);
	}
	
	public boolean isSSL() {
		return "imaps".equals(getProtocol()) ? true : false;
	}

	public Properties getProperties() {
		return properties;
	}

	public Session getSession() {
		if (session == null) {
			session = Session.getInstance(properties);
		}
		return session;
	}

	public URLName getURLName() {
		return new URLName(getProtocol(), getHost(), getPort(), getMailbox(), getUsername(), getPassword());
	}

	public Store getConnectedStore() throws MessagingException {
		if (store == null) {
			store = getSession().getStore(getURLName());
			store.connect();
		}
		return store;
	}

	public boolean isDebug() {
		return debugEnabled;
	}

	public void setDebug(boolean enabled) {
		debugEnabled = enabled;
		properties.put(MAIL_DEBUG, enabled ? "true" : "false");
	}

	public Folder getMailboxFolder() throws MessagingException {
		getConnectedStore();
		return store.getFolder(mailbox);
	}

	public void setEmailAddress(String addr) {
		if (addr == null) {
			if (getUsername() != null && getUsername().contains("@")) {
				addr = getUsername();
			}
			else {
				addr = getUsername() + "@" + getHost();		
			}
		}
		properties.put(MAIL_FROM, addr);
	}
	
	public String getEmailAddress() {
		return properties.getProperty(MAIL_FROM);
	}

}
