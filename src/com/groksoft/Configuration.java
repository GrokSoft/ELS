package com.groksoft;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Configuration
 * <p>
 * Contains all command-line options and any other application-level configuration.
 */
public class Configuration
{
    private final String VOLMONGER_VERSION = "1.0.0";
    private Logger logger = LogManager.getLogger("applog");

    // flags & names
    private String debugLevel = "info";
    private boolean keepVolMongerFiles = false;
    private String logFilename = "VolMonger.log";
    private boolean testRun = false;
    private boolean validationRun = false;

    // publisher & subscriber
    private String publisherFileName = "";
    private String publisherLibraryName = "";
    private boolean specificPublisherLibrary = false;
    private String subscriberName = "";
    private String subscriberFileName = "";

    /**
     * Instantiates a new Configuration.
     */
    public Configuration() {

    }

    /**
     * Gets log filename.
     *
     * @return the log filename
     */
    public String getLogFilename() {
        return logFilename;
    }

    /**
     * Sets log filename.
     *
     * @param logFilename the log filename
     */
    public void setLogFilename(String logFilename) {
        this.logFilename = logFilename;
    }

    /**
     * Parse command line.
     *
     * @param args the args
     * @return the boolean
     * @throws MongerException the monger exception
     */
    public boolean parseCommandLine(String[] args) throws MongerException {
        int index;
        boolean success = true;

        for (index = 0; index < args.length - 1; ++index) {
//            switch (argv[index]) {
//                case 1:
//                    break;
//            }
        }
        return success;
    }

    /**
     * Gets VolMonger version.
     *
     * @return the VolMonger version
     */
    public String getVOLMONGER_VERSION() {
        return VOLMONGER_VERSION;
    }

    /**
     * Gets debug level.
     *
     * @return the debug level
     */
    public String getDebugLevel() {
        return debugLevel;
    }

    /**
     * Sets debug level.
     *
     * @param debugLevel the debug level
     */
    public void setDebugLevel(String debugLevel) {
        this.debugLevel = debugLevel;
    }

    /**
     * Is keep vol monger files boolean.
     *
     * @return the boolean
     */
    public boolean isKeepVolMongerFiles() {
        return keepVolMongerFiles;
    }

    /**
     * Sets keep vol monger files.
     *
     * @param keepVolMongerFiles the keep vol monger files
     */
    public void setKeepVolMongerFiles(boolean keepVolMongerFiles) {
        this.keepVolMongerFiles = keepVolMongerFiles;
    }

    /**
     * Gets publisher configuration file name.
     *
     * @return the publisher configuration file name
     */
    public String getPublisherFileName() {
        return publisherFileName;
    }

    /**
     * Sets publisher configuration file name.
     *
     * @param publisherFileName the publisher configuration file name
     */
    public void setPublisherFileName(String publisherFileName) {
        this.publisherFileName = publisherFileName;
    }

    /**
     * Gets publisher library name.
     *
     * @return the publisher library name
     */
    public String getPublisherLibraryName() {
        return publisherLibraryName;
    }

    /**
     * Sets publisher library name.
     *
     * @param publisherLibraryName the publisher library name
     */
    public void setPublisherLibraryName(String publisherLibraryName) {
        this.publisherLibraryName = publisherLibraryName;
    }

    /**
     * Is specific publisher library boolean.
     *
     * @return the boolean
     */
    public boolean isSpecificPublisherLibrary() {
        return specificPublisherLibrary;
    }

    /**
     * Sets specific publisher library.
     *
     * @param specificPublisherLibrary the specific publisher library
     */
    public void setSpecificPublisherLibrary(boolean specificPublisherLibrary) {
        this.specificPublisherLibrary = specificPublisherLibrary;
    }

    /**
     * Gets subscriber configuration file name.
     *
     * @return the subscriber configuration file name
     */
    public String getSubscriberFileName() {
        return subscriberFileName;
    }

    /**
     * Sets subscriber configuration file name.
     *
     * @param subscriberFileName the subscriber configuration file name
     */
    public void setSubscriberFileName(String subscriberFileName) {
        this.subscriberFileName = subscriberFileName;
    }

    /**
     * Gets subscriber name.
     *
     * @return the subscriber name
     */
    public String getSubscriberName() {
        return subscriberName;
    }

    /**
     * Sets subscriber name.
     *
     * @param subscriberName the subscriber name
     */
    public void setSubscriberName(String subscriberName) {
        this.subscriberName = subscriberName;
    }

    /**
     * Is test run boolean.
     *
     * @return the boolean
     */
    public boolean isTestRun() {
        return testRun;
    }

    /**
     * Sets test run.
     *
     * @param testRun the test run
     */
    public void setTestRun(boolean testRun) {
        this.testRun = testRun;
    }

    /**
     * Is validation run boolean.
     *
     * @return the boolean
     */
    public boolean isValidationRun() {
        return validationRun;
    }

    /**
     * Sets validation run.
     *
     * @param validationRun the validation run
     */
    public void setValidationRun(boolean validationRun) {
        this.validationRun = validationRun;
    }

}