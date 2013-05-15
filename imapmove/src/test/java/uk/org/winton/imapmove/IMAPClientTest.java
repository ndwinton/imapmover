package uk.org.winton.imapmove;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Properties;

import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.URLName;

import org.junit.Before;
import org.junit.Test;
import org.jvnet.mock_javamail.Mailbox;

public class IMAPClientTest {

	private static final String FULL_URL_STRING = "imaps://username:password@server:9999/mailbox";

	@Before
	public void setUp() throws Exception {
		Mailbox.clearAll();
	}

	@Test
	public void shouldBeAbleToCreateIMAPMoverUsingFullStringURLName() {
		IMAPClient client = new IMAPClient(new URLName(FULL_URL_STRING));
		assertEquals("username", client.getUsername());
		assertEquals("password", client.getPassword());
		assertEquals("server", client.getHost());
		assertEquals(9999, client.getPort());
		assertEquals("mailbox", client.getMailbox());
		assertTrue("expecting SSL", client.isSSL());
	}

	@Test
	public void shouldBeAbleToCreateIMAPMoverUsingString() {
		IMAPClient client = new IMAPClient(FULL_URL_STRING);
		assertEquals("username", client.getUsername());
		assertEquals("password", client.getPassword());
		assertEquals("server", client.getHost());
		assertEquals(9999, client.getPort());
		assertEquals("mailbox", client.getMailbox());
		assertTrue("expecting SSL", client.isSSL());
	}
	
	@Test
	public void constructorShouldGenerateSetOfPropertiesFromFullURLName() {
		IMAPClient client = new IMAPClient(new URLName(FULL_URL_STRING));
		Properties props = client.getProperties();
		assertNotNull(props);
		assertEquals("username", props.get("mail.user"));
		assertEquals("password", props.get("mail.password"));
		assertEquals("server", props.get("mail.host"));
		assertEquals("9999", props.get("mail.port"));
		assertEquals("imaps", props.get("mail.store.protocol"));
		assertEquals("username@server", props.get("mail.from"));
	}
	
	@Test
	public void shouldBeAbleToEnableAndDisableDebug() {
		IMAPClient client = new IMAPClient(new URLName(FULL_URL_STRING));
		Properties props = client.getProperties();
		assertFalse(client.isDebug());
		assertEquals("false", props.get("mail.debug"));
		client.setDebug(true);
		assertEquals("true", props.get("mail.debug"));
		assertTrue(client.isDebug());
	}
	
	@Test
	public void shouldSetDefaultPortForIMAP() {
		IMAPClient client = new IMAPClient("imap://localhost/mailbox");
		assertEquals(143, client.getPort());
	}

	@Test
	public void shouldSetDefaultPortForIMAPS() {
		IMAPClient client = new IMAPClient("imaps://localhost/mailbox");
		assertEquals(993, client.getPort());
	}
	
	@Test
	public void shouldSetDefaultMailboxPath() {
		IMAPClient client = new IMAPClient("imap://localhost");
		assertEquals("INBOX", client.getMailbox());		
	}
	
	@Test
	public void shouldUseLocalhostForDefaultHost() {
		IMAPClient client = new IMAPClient("imap://foo/mailbox");
		client.setHost(null);
		assertEquals("localhost", client.getHost());
	}
	
	@Test
	public void sslEnablePropertyShouldBeSetAccordingToProtocolType() {
		IMAPClient client = new IMAPClient("imaps://localhost");
		assertEquals("true", client.getProperties().getProperty("mail.imap.ssl.enable"));
		client = new IMAPClient("imap://localhost");
		assertEquals("false", client.getProperties().getProperty("mail.imap.ssl.enable"));		
	}
	
	@Test
	public void shouldUseIMAPForDefaultProtocol() {
		IMAPClient client = new IMAPClient("imaps://localhost");
		client.setProtocol(null);
		assertEquals("imap", client.getProtocol());
	}
	
	@Test
	public void shouldGetFullURLNameConstructedFromParts() {
		IMAPClient client = new IMAPClient("imap://original");
		client.setProtocol("imaps");
		client.setHost("newhost");
		client.setUsername("username@somewhere");
		client.setPassword("password");
		client.setMailbox("mbox");
		client.setPort(1234);
		assertEquals("imaps://username%40somewhere:password@newhost:1234/mbox", client.getURLName().toString());
	}
	
	@Test
	public void shouldCreateNewSessionIfNoneExists() {
		
		IMAPClient client = new IMAPClient("imap://localhost");
		Session s1 = client.getSession();
		assertNotNull(s1);
		Session s2 = client.getSession();
		assertEquals(s1, s2);
	}

	@Test
	public void shouldBeAbleToGetConnectedStoreDirectly() throws MessagingException {		
		IMAPClient client = new IMAPClient("imap://username:password@localhost");
		Store s1 = client.getConnectedStore();
		assertNotNull(s1);
		Store s2 = client.getConnectedStore();
		assertEquals(s1, s2);
	}

	@Test 
	public void shouldBeAbleToGetMailboxFolderObjectDirectly() throws MessagingException {
		
		IMAPClient client = new IMAPClient("imap://localhost/mailbox");
		Folder f1 = client.getMailboxFolder();
		assertNotNull(f1);
	}
	
	@Test
	public void shouldConstructDefaultEmailAddressIfNoneSpecified() {
		IMAPClient client = new IMAPClient("imap://someuser:pwd@somehost.com:1234/");
		assertEquals("someuser@somehost.com", client.getEmailAddress());
	}
	
	@Test
	public void shouldExtractDefaultEmailAddressfromURLIfApparentlyPresent() {
		IMAPClient client = new IMAPClient("imap://someuser%40mycompany.com@somehost.com:1234/");
		assertEquals("someuser@mycompany.com", client.getEmailAddress());
	}
	
	@Test
	public void shouldBeAbleToOverrideDefaultEmailAddress() {
		IMAPClient client = new IMAPClient("imap://someuser%40mycompany.com@somehost.com:1234/");
		client.setEmailAddress("other@elsewhere.com");
		assertEquals("other@elsewhere.com", client.getEmailAddress());
		
	}
}
