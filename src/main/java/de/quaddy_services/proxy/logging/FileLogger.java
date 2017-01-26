package de.quaddy_services.proxy.logging;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;

/**
 * 
 */
public class FileLogger {
	private File file;

	FileLogger(File aFile) {
		file = aFile;
	}

	/**
	 * 
	 */
	public synchronized void log(String aPrefix, String aLevel, String aMessage, Throwable aE) {
		try {
			FileWriter tempFileWriter = new FileWriter(file, true);
			try {
				String tempText = LocalDateTime.now().toString() + ":" + aLevel + ":" + aPrefix + ":" + aMessage;
				System.out.println(tempText);
				tempFileWriter.write(tempText);
				if (aE != null) {
					tempFileWriter.write(System.lineSeparator());
					aE.printStackTrace(new PrintWriter(tempFileWriter));
					aE.printStackTrace();
				}
				tempFileWriter.write(System.lineSeparator());
			} finally {
				tempFileWriter.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
