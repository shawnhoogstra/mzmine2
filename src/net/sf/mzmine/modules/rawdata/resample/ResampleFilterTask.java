/*
 * Copyright 2006-2009 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.rawdata.resample;

import java.io.IOException;
import java.util.logging.Logger;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.RawDataFileWriter;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.data.impl.SimpleDataPoint;
import net.sf.mzmine.data.impl.SimpleScan;
import net.sf.mzmine.main.mzmineclient.MZmineCore;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.Range;
import net.sf.mzmine.util.ScanUtils;

/**
 * 
 */
class ResampleFilterTask implements Task {

	private Logger logger = Logger.getLogger(this.getClass().getName());
	
	private RawDataFile dataFile, filteredRawDataFile;

	private TaskStatus status = TaskStatus.WAITING;
	private String errorMessage;

	// scan counter
	private int filteredScans, totalScans;

	// parameter values
	private String suffix;
	private Double binSize;
	private boolean removeOriginal;

	/**
	 * @param rawDataFile
	 * @param parameters
	 */
	ResampleFilterTask(RawDataFile dataFile, ResampleFilterParameters parameters) {
		this.dataFile = dataFile;
		suffix = (String) parameters
				.getParameterValue(ResampleFilterParameters.suffix);
		binSize = (Double) parameters
				.getParameterValue(ResampleFilterParameters.binSize);
		removeOriginal = (Boolean) parameters
				.getParameterValue(ResampleFilterParameters.autoRemove);
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
	 */
	public String getTaskDescription() {
		return "m/z resample filtering " + dataFile;
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
	 */
	public double getFinishedPercentage() {
		if (totalScans == 0)
			return 0.0f;
		return (double) filteredScans / totalScans;
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getStatus()
	 */
	public TaskStatus getStatus() {
		return status;
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getErrorMessage()
	 */
	public String getErrorMessage() {
		return errorMessage;
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#cancel()
	 */
	public void cancel() {
		status = TaskStatus.CANCELED;
	}

	/**
	 * @see java.lang.Runnable#run()
	 */
	public void run() {

		status = TaskStatus.PROCESSING;
		logger.info("Running resample filter on " + dataFile);

		try {

			// Create new temporary file
			String newName = dataFile.getName() + " " + suffix;
			RawDataFileWriter rawDataFileWriter = MZmineCore
					.createNewFile(newName);

			// Get all scans
			int[] scanNumbers = dataFile.getScanNumbers();
			totalScans = scanNumbers.length;

			// Loop through all scans
			for (int scanIndex = 0; scanIndex < totalScans; scanIndex++) {

				// Check if we are not canceled
				if (status == TaskStatus.CANCELED)
					return;

				// Get scan
				Scan oldScan = dataFile.getScan(scanNumbers[scanIndex]);
				Range mzRange = oldScan.getMZRange();
				int numberOfBins = (int) Math.round((mzRange.getMax() - mzRange
						.getMin())
						/ binSize);
				if (numberOfBins == 0) {
					numberOfBins++;
				}

				// ScanUtils.binValues needs arrays
				DataPoint dps[] = oldScan.getDataPoints();
				double[] x = new double[dps.length];
				double[] y = new double[dps.length];
				for (int i = 0; i < dps.length; i++) {
					x[i] = dps[i].getMZ();
					y[i] = dps[i].getIntensity();
				}
				// the new intensity values
				double[] newY = ScanUtils.binValues(x, y, mzRange,
						numberOfBins, !oldScan.isCentroided(),
						ScanUtils.BinningType.AVG);
				SimpleDataPoint[] newPoints = new SimpleDataPoint[newY.length];

				// set the new m/z value in the middle of the bin
				double newX = mzRange.getMin() + binSize / 2.0;
				// creates new DataPoints
				for (int i = 0; i < newY.length; i++) {
					newPoints[i] = new SimpleDataPoint(newX, newY[i]);
					newX += binSize;
				}

				// Create updated scan
				SimpleScan newScan = new SimpleScan(oldScan);
				newScan.setDataPoints(newPoints);

				// Write the updated scan to new file
				rawDataFileWriter.addScan(newScan);
				filteredScans++;
			}

			// Finalize writing
			filteredRawDataFile = rawDataFileWriter.finishWriting();
			MZmineCore.getCurrentProject().addFile(filteredRawDataFile);

			// Remove the original file if requested
			if (removeOriginal)
				MZmineCore.getCurrentProject().removeFile(dataFile);

			status = TaskStatus.FINISHED;
			logger.info("Finished m/z resample filter on " + dataFile);

		} catch (IOException e) {
			status = TaskStatus.ERROR;
			errorMessage = e.toString();
			return;
		}

	}
	
	public Object[] getCreatedObjects() {
		return new Object[] { filteredRawDataFile };
	}
		
}