package de.flohrit.drillrig.runtime;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.GregorianCalendar;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.sshd.SshServer;
import org.apache.sshd.common.util.OsUtils;
import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.shell.ProcessShellFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.flohrit.drillrig.config.Configuration;
import de.flohrit.drillrig.config.Security;
import de.flohrit.drillrig.config.User;
import de.flohrit.drillrig.util.StringEncDecoder;
import de.flohrit.drillrig.util.StringUtils;

public class DrillServer {
	final static private Logger logger = LoggerFactory
			.getLogger(DrillServer.class);

	private static SshSessionManager sshClientManager;
	private static Configuration configuration;
	private static StringEncDecoder encDecoder;

	public static String getAnonymizedString(String val, int readable) {
		if (val.length() - readable <= 0) {
			return val;
		}

		byte[] bytes = val.getBytes();
		Arrays.fill(bytes, 0, val.length() - readable, "*".getBytes()[0]);
		return new String(bytes);
	}

	public static SshSessionManager getSshClientManager() {
		return sshClientManager;
	}

	public static Configuration getConfiguration() {
		return configuration;
	}

	/**
	 * Read server configuration file.
	 * 
	 * @return Configuration object.
	 */
	public static void readConfiguration() {
		try {
			File configFile = new File("config.xml");
			if (configFile.isFile()) {
				JAXBContext jc = JAXBContext
						.newInstance("de.flohrit.drillrig.config");

				Unmarshaller um = jc.createUnmarshaller();

				Configuration newConfiguration = (Configuration) um
						.unmarshal(configFile);
				configuration = newConfiguration;

			} else {
				configuration = new Configuration();

				GregorianCalendar gcal = new GregorianCalendar();
				XMLGregorianCalendar xgcal = DatatypeFactory.newInstance()
						.newXMLGregorianCalendar(gcal);

				configuration.setCreationDate(xgcal);
				configuration.setModificationDate(xgcal);
				configuration.setVersion("0.1");
				configuration.setId(StringUtils.createUUID());
				InetAddress addr;
				try {
					addr = InetAddress.getLocalHost();
					configuration.setHost( addr.getHostName());
				} catch (UnknownHostException e) {
					configuration.setHost("unknown");
				}

				User user = new User();
				user.setName("admin");
				user.setEmail("admin@localhost");
				user.setEnabled(true);
				user.setPassword("");
				user.getRole().add("admin");
				user.getRole().add("user");

				configuration.setSecurity(new Security());
				configuration.getSecurity().getUser().add(user);

				writeConfiguration(configuration);
			}

		} catch (JAXBException e3) {
			logger.error("Error reading configuration file config.xml", e3);
		} catch (DatatypeConfigurationException e) {
			logger.error("XMLGregorianCalendar not found", e);
		}
	}

	/**
	 * 
	 */
	public static void restartService() {

		if (sshClientManager != null) {
			sshClientManager.stop();
		}

		readConfiguration();

		sshClientManager = new SshSessionManager(configuration.getSshSession());
		sshClientManager.start();
	}

	/**
	 * Write configuration file config.xml
	 * 
	 * @param configuration
	 *            to write
	 */
	public static void writeConfiguration(Configuration configuration) {
		File configFile = new File("config.xml");
		File backFile = new File("config.xml.tmp");
		FileOutputStream config = null;

		try {
			JAXBContext jc = JAXBContext
					.newInstance("de.flohrit.drillrig.config");
			Marshaller m = jc.createMarshaller();

			m.setProperty("jaxb.formatted.output", true);

			config = new FileOutputStream(backFile);
			m.marshal(configuration, config);
			config.close();

			if (!configFile.exists() || configFile.delete()) {
				backFile.renameTo(configFile);
			}

		} catch (Exception e) {
			logger.error("Error writing configuration file config.xml", e);
		} finally {
			if (config != null) {
				try {
					config.close();
				} catch (IOException e1) {
				}
			}
		}
	}

	public static StringEncDecoder getEncDecorder() {
		if (encDecoder == null) {
			encDecoder = new StringEncDecoder(getConfiguration());
		}

		return encDecoder;
	}
	
	public static void startService() {
		SshServer sshd = SshServer.setUpDefaultServer();
		sshd.setPort(3222);
		sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(
				"hostkey.ser"));

		sshd.setPasswordAuthenticator(new PasswordAuthenticator() {

			@Override
			public boolean authenticate(String paramString1,
					String paramString2, ServerSession paramServerSession) {
				// hi
				return true;
			}
		});

		if (OsUtils.isUNIX()) {
			sshd.setShellFactory(new ProcessShellFactory(new String[] {
					"/bin/sh", "-i", "-l" }, EnumSet
					.of(ProcessShellFactory.TtyOptions.ONlCr)));
		} else {
			sshd.setShellFactory(new ProcessShellFactory(
					new String[] { "cmd.exe " }, EnumSet.of(
							ProcessShellFactory.TtyOptions.Echo,
							ProcessShellFactory.TtyOptions.ICrNl,
							ProcessShellFactory.TtyOptions.ONlCr)));
		}
/*
		try {
			sshd.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
*/		
		restartService();
	}
	
	public static void stopService() {
		
		if (sshClientManager != null) {
			sshClientManager.stop();
			sshClientManager=null;
		}
	}
}
