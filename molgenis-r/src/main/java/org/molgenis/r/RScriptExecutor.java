package org.molgenis.r;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Executes a r script with the RScript executable in a new process.
 * 
 */
@Service
public class RScriptExecutor
{
	private static final Logger logger = Logger.getLogger(RScriptExecutor.class);
	private final String rScriptExecutable;

	@Autowired
	public RScriptExecutor(@Value("${r_script_executable:/usr/bin/Rscript}") String rScriptExecutable)
	{
		if (rScriptExecutable == null)
		{
			throw new IllegalArgumentException("rExecutable is null");
		}

		this.rScriptExecutable = rScriptExecutable;
	}

	/**
	 * Execute a r script and wait for it to finish
	 */
	public void executeScript(File script, ROutputHandler outputHandler)
	{
		// Check if r is installed
		File file = new File(rScriptExecutable);
		if (!file.exists())
		{
			throw new MolgenisRException("File [" + rScriptExecutable + "] does not exist");
		}

		// Check if r has execution rights
		if (!file.canExecute())
		{
			throw new MolgenisRException("Can not execute [" + rScriptExecutable
					+ "]. Does it have executable permissions?");
		}

		// Check if the r script exists
		if (!script.exists())
		{
			throw new MolgenisRException("File [" + script + "] does not exist");
		}

		try
		{
			// Create r process
			logger.info("Running r script [" + script.getAbsolutePath() + "]");
			Process process = Runtime.getRuntime().exec(rScriptExecutable + " " + script.getAbsolutePath());

			// Capture the error output
			final StringBuilder sb = new StringBuilder();
			RStreamHandler errorHandler = new RStreamHandler(process.getErrorStream(), new ROutputHandler()
			{
				@Override
				public void outputReceived(String output)
				{
					sb.append(output).append("\n");
				}
			});
			errorHandler.start();

			// Capture r output if an r output handler is defined
			if (outputHandler != null)
			{
				RStreamHandler streamHandler = new RStreamHandler(process.getInputStream(), outputHandler);
				streamHandler.start();
			}

			// Wait until script is finished
			process.waitFor();

			// Check for errors
			if (process.exitValue() > 0)
			{
				throw new MolgenisRException("Error running [" + script.getAbsolutePath() + "]." + sb.toString());
			}

			logger.info("Script [" + script.getAbsolutePath() + "] done");
		}
		catch (IOException e)
		{
			throw new MolgenisRException("Exception executing RScipt.", e);
		}
		catch (InterruptedException e)
		{
			throw new MolgenisRException("Exception waiting for RScipt to finish", e);
		}
	}
}
