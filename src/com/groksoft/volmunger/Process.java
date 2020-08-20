package com.groksoft.volmunger;

import com.groksoft.volmunger.repository.Item;
import com.groksoft.volmunger.repository.Library;
import com.groksoft.volmunger.storage.Storage;
import com.groksoft.volmunger.storage.Target;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.*;
import java.util.ArrayList;

/**
 * VolMunger Process
 */
public class Process
{
    private Configuration cfg = null;
    private Main.Context context;
    private int copyCount = 0;
    private String currentGroupName = "";
    private int errorCount = 0;
    private long grandTotalItems = 0L;
    private long grandTotalOriginalLocation = 0L;
    private long grandTotalSize = 0L;
    private ArrayList<String> ignoredList = new ArrayList<>();
    private String lastGroupName = "";
    private transient Logger logger = LogManager.getLogger("applog");
    private Storage storageTargets = null;
    private long whatsNewTotal = 0;

    /**
     * Hide default constructor
     */
    private Process()
    {
    }

    /**
     * Instantiates the class
     */
    public Process(Configuration config, Main.Context ctxt)
    {
        this.cfg = config;
        this.context = ctxt;
    }

    /**
     * Copy a file, local or remote
     *
     * @param from the from
     * @param to   the to
     * @return the boolean
     */
    public boolean copyFile(String from, String to, boolean overwrite)
    {
        try
        {
            if (cfg.isRemoteSession())
            {
                context.clientSftp.transmitFile(from, to, overwrite);
            }
            else
            {
                Path fromPath = Paths.get(from).toRealPath();
                Path toPath = Paths.get(to);  //.toRealPath();
                File f = new File(to);
                if (f != null)
                {
                    f.getParentFile().mkdirs();
                }
                Files.copy(fromPath, toPath, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING, LinkOption.NOFOLLOW_LINKS);
            }
        }
        catch (UnsupportedOperationException e)
        {
            logger.error("Copy problem UnsupportedOperationException: " + e.getMessage());
            return false;
        }
        catch (FileAlreadyExistsException e)
        {
            logger.error("Copy problem FileAlreadyExistsException: " + e.getMessage());
            return false;
        }
        catch (DirectoryNotEmptyException e)
        {
            logger.error("Copy problem DirectoryNotEmptyException: " + e.getMessage());
            return false;
        }
        catch (IOException e)
        {
            logger.error("Copy problem IOException: " + e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * Copy group of files
     * <p>
     * The overwrite parameter is false for normal Process munge operations, and
     * true for Subscriber terminal (-r T) to Publisher listener (-r L) operations.
     *
     * @param group     the group
     * @param totalSize the total size
     * @param overwrite whether to overwrite any existing target file
     * @throws MungerException the volmunger exception
     */
    public String copyGroup(ArrayList<Item> group, long totalSize, boolean overwrite) throws MungerException
    {
        String response = "";
        if (storageTargets == null && cfg.getTargetsFilename().length() > 0)
        {
            getStorageTargets();
        }
        else
        {
            throw new MungerException("-t or -T target are required for this operation");
        }
        try
        {
            if (group.size() > 0)
            {
                for (Item groupItem : group)
                {
                    if (cfg.isDryRun())
                    {
                        // -D Dry run option
                        ++copyCount;
                        logger.info("  > Would copy #" + copyCount + " " + groupItem.getFullPath());
                    }
                    else
                    {
                        String targetPath = getTarget(groupItem, groupItem.getLibrary(), totalSize);
                        if (targetPath != null)
                        {
                            // copy item(s) to targetPath
                            ++copyCount;

                            String to = targetPath + context.subscriberRepo.getWriteSeparator();
                            to += context.publisherRepo.normalize(context.subscriberRepo.getLibraryData().libraries.flavor, groupItem.getItemPath());

                            String msg = "  > Copying #" + copyCount + " " + groupItem.getFullPath() + " to " + to;
                            logger.info(msg);
                            response += (msg + "\r\n");

                            if (!copyFile(groupItem.getFullPath(), to, overwrite))
                            {
                                ++errorCount;
                            }
                        }
                        else
                        {
                            logger.error("    No space on any targetPath " + group.get(0).getLibrary() + " for " +
                                    lastGroupName + " that is " + totalSize / (1024 * 1024) + " MB");
                        }
                    }
                }
            }
            grandTotalItems = grandTotalItems + group.size();
            group.clear();
            grandTotalSize = grandTotalSize + totalSize;
            totalSize = 0L;
            lastGroupName = currentGroupName;
        }
        catch (Exception e)
        {
            throw new MungerException(e.getMessage() + " trace: " + Utils.getStackTrace(e));
        }

        return response;
    }

    /**
     * Export publisher collection data to configured file as JSON
     */
    private void exportCollection()
    {
        try
        {
            for (Library pubLib : context.publisherRepo.getLibraryData().libraries.bibliography)
            {
                context.publisherRepo.scan(pubLib.name);
            }
            context.publisherRepo.exportCollection();
        }
        catch (MungerException e)
        {
            // no logger yet to just print to the screen
            System.out.println(e.getMessage());
        }
    }

    /**
     * Export publisher collection data to configured file as plain text
     */
    private void exportText()
    {
        try
        {
            for (Library pubLib : context.publisherRepo.getLibraryData().libraries.bibliography)
            {
                context.publisherRepo.scan(pubLib.name);
            }
            context.publisherRepo.exportText();
        }
        catch (MungerException e)
        {
            // no logger yet to just print to the screen
            System.out.println(e.getMessage());
        }
    }

    public int getCopyCount()
    {
        return copyCount;
    }

    /**
     * Get item size as long
     *
     * @param item the item root node
     * @return the long size of the item(s) in bytes
     */
    public long getItemSize(Item item)
    {
        long size = 0;
        try
        {
            size = Files.size(Paths.get(item.getFullPath()));
        }
        catch (IOException e)
        {
            logger.error("Exception '" + e.getMessage() + "' getting size of item " + item.getFullPath());
        }
        return size;
    }

    public void getStorageTargets() throws MungerException
    {
        String location = cfg.getTargetsFilename();

        if (cfg.isRemoteSession() && cfg.isRequestTargets())
        {
            // request target data from remote subscriber
            location = context.clientStty.retrieveRemoteData(location, "targets");
            cfg.setTargetsFilename(location);
        }

        storageTargets.read(location, context.subscriberRepo.getLibraryData().libraries.flavor);
        if (!cfg.isRemoteSession())
            storageTargets.validate();
    }

    /**
     * Gets a subscriber target
     * <p>
     * Will return the original directory where existing files are located if one
     * exists and that location has enough space.
     * <p>
     * Otherwise will return one of the subscriber targets for the library of the item
     * that has enough space to hold the item, otherwise an empty string is returned.
     *
     * @param item    the item
     * @param library the publisher library.definition.name
     * @param size    the total size of item(s) to be copied
     * @return the target
     * @throws MungerException the volmunger exception
     */
    public String getTarget(Item item, String library, long size) throws MungerException
    {
        String target = null;
        boolean allFull = true;
        boolean notFound = true;
        long space = 0L;
        long minimum = 0L;

        Target tar = storageTargets.getLibraryTarget(library);
        if (tar != null)
        {
            minimum = Utils.getScaledValue(tar.minimum);
        }
        else
        {
            minimum = Storage.minimumBytes;
        }

        // see if there is an "original" directory the new content will fit in
        String path = context.subscriberRepo.hasDirectory(library, Utils.pipe(context.publisherRepo, item.getItemPath()));
        if (path != null)
        {
            if (cfg.isRemoteSession())
            {
                // remote subscriber
                space = context.clientStty.availableSpace(path);
            }
            else
            {
                space = Utils.availableSpace(path);
            }
            logger.info("Checking space on " + (cfg.isRemoteSession() ? "remote" : "local") +
                    " path " + path + " == " + (Utils.formatLong(space)) +
                    " for " + (Utils.formatLong(size)) +
                    " minimum " + Utils.formatLong(minimum));
            if (space > (size + minimum))
            {
                logger.info("Using original storage location for " + item.getItemPath() + " at " + path);
                //
                // inline return
                //
                ++grandTotalOriginalLocation;
                return path;
            }
            else
            {
                logger.info("Original storage location too full for " + item.getItemPath() + " (" + size + ") at " + path);
            }
        }

        // find a matching target
        if (tar != null)
        {
            notFound = false;
            for (int j = 0; j < tar.locations.length; ++j)
            {
                // check space on the candidate target
                String candidate = tar.locations[j];
                if (cfg.isRemoteSession())
                {
                    // remote subscriber
                    space = context.clientStty.availableSpace(candidate);
                }
                else
                {
                    space = Utils.availableSpace(candidate);
                }
                if (space > minimum)
                {
                    // check target space minimum
                    allFull = false;
                    if (space > (size + minimum))
                    {
                        // check size of item(s) to be copied
                        target = candidate;             // has space, use it
                        break;
                    }
                }
            }
            if (allFull)
            {
                logger.error("All locations for library " + library + " are below specified minimum of " + tar.minimum);

                // todo Should this be a throw ??
                System.exit(2);     // EXIT the program


            }
        }
        if (notFound)
        {
            logger.error("No target library match found for publisher library " + library);
        }
        return target;
    }

    /**
     * Is new grouping boolean
     * <p>
     * True if the item "group" is different than the current "group".
     * A group is a set of files within the same movie directory or
     * television season.
     *
     * @param publisherItem the publisher item
     * @return the boolean
     */
    private boolean isNewGrouping(Item publisherItem) throws MungerException
    {
        boolean ret = true;
        String p = publisherItem.getItemPath();
        String s = context.publisherRepo.getSeparator();
        int i = publisherItem.getItemPath().lastIndexOf(context.publisherRepo.getSeparator());
        if (i < 0)
        {
            logger.warn("No subdirectory in path : " + publisherItem.getItemPath());
            return true;
        }
        String path = publisherItem.getItemPath().substring(0, i);
        if (path.length() < 1)
        {
            path = publisherItem.getItemPath().substring(0, publisherItem.getItemPath().lastIndexOf(context.publisherRepo.getSeparator()));
        }
        if (currentGroupName.equalsIgnoreCase(path))
        {
            ret = false;
        }
        else
        {
            currentGroupName = path;
        }
        return ret;
    }

    /**
     * Munge two collections
     * <p>
     * This is the full munge process.
     *
     * @throws MungerException the volmunger exception
     */
    private void munge() throws MungerException
    {
        Item lastDirectoryItem = null;
        PrintWriter mismatchFile = null;
        PrintWriter whatsNewFile = null;
        PrintWriter targetFile = null;
        String currentWhatsNew = "";
        String currLib = "";
        ArrayList<Item> group = new ArrayList<>();
        long totalSize = 0;

        String header = "Munging " + context.publisherRepo.getLibraryData().libraries.description + " to " +
                context.subscriberRepo.getLibraryData().libraries.description;

        // setup the -m mismatch output file
        if (cfg.getMismatchFilename().length() > 0)
        {
            try
            {
                mismatchFile = new PrintWriter(cfg.getMismatchFilename());
                mismatchFile.println(header);
                logger.info("Writing to Mismatches file " + cfg.getMismatchFilename());
            }
            catch (FileNotFoundException fnf)
            {
                String s = "File not found exception for Mismatches output file " + cfg.getMismatchFilename();
                logger.error(s);
                throw new MungerException(s);
            }
        }

        // setup the -n What's New output file
        if (cfg.getWhatsNewFilename().length() > 0)
        {
            try
            {
                whatsNewFile = new PrintWriter(cfg.getWhatsNewFilename());
                whatsNewFile.println("What's New");
                logger.info("Writing to What's New file " + cfg.getWhatsNewFilename());
            }
            catch (FileNotFoundException fnf)
            {
                String s = "File not found exception for What's New output file " + cfg.getWhatsNewFilename();
                logger.error(s);
                throw new MungerException(s);
            }
        }

        logger.info(header);

        try
        {
            for (Library subLib : context.subscriberRepo.getLibraryData().libraries.bibliography)
            {
                boolean scanned = false;
                Library pubLib = null;

                // TODO Add filtering which library to process by -l option, isSpecificPublisherLibrary() & getPublisherLibraryNames()

                if ((pubLib = context.publisherRepo.getLibrary(subLib.name)) != null)
                {
                    // Do the libraries have items or do they need to be scanned?
                    if (pubLib.items == null || pubLib.items.size() < 1)
                    {
                        context.publisherRepo.scan(pubLib.name);
                        scanned = true;
                    }
                    if (subLib.items == null || subLib.items.size() < 1)
                    {
                        if (cfg.isRemoteSession())
                        {
                            throw new MungerException("Subscriber collection missing data for subscriber library " + subLib.name);
                        }
                        context.subscriberRepo.scan(subLib.name);
                    }

                    logger.info("Munge " + subLib.name + ": " + pubLib.items.size() + " publisher items with " +
                            subLib.items.size() + " subscriber items");

                    for (Item item : pubLib.items)
                    {
                        if (context.publisherRepo.ignore(item))
                        {
                            logger.info("  ! Ignoring '" + item.getItemPath() + "'");
                            ignoredList.add(item.getFullPath());
                        }
                        else
                        {
                            boolean has = context.subscriberRepo.hasItem(subLib.name, Utils.pipe(context.publisherRepo, item.getItemPath()));
                            if (has)
                            {
                                logger.info("  = Subscriber " + subLib.name + " has " + item.getItemPath());
                            }
                            else
                            {

                                if (cfg.getWhatsNewFilename().length() > 0)
                                {
                                    /*
                                     * Only show the left side of mismatches file. And Only show it once.
                                     * So if you have 10 new episodes of Lucifer only the following will show in the what's new file
                                     * Big Bang Theory
                                     * Lucifer
                                     * Legion
                                     */
                                    if (!item.getLibrary().equals(currLib))
                                    {
                                        // If not first time display and reset the whatsNewTotal
                                        if (!currLib.equals(""))
                                        {
                                            whatsNewFile.println("--------------------------------");
                                            whatsNewFile.println("Total for " + currLib + " = " + whatsNewTotal);
                                            whatsNewFile.println("================================");
                                            whatsNewTotal = 0;
                                        }
                                        currLib = item.getLibrary();
                                        whatsNewFile.println("");
                                        whatsNewFile.println(currLib);
                                        whatsNewFile.println(new String(new char[currLib.length()]).replace('\0', '='));
                                    }
                                    String path;
                                    path = Utils.getLastPath(item.getItemPath(), context.publisherRepo.getSeparator());
                                    if (!currentWhatsNew.equalsIgnoreCase(path))
                                    {
                                        assert whatsNewFile != null;
                                        whatsNewFile.println("    " + path);
                                        currentWhatsNew = path;
                                        whatsNewTotal++;
                                    }
                                }

                                logger.info("  + Subscriber " + subLib.name + " missing " + item.getItemPath());

                                if (!item.isDirectory())
                                {
                                    if (cfg.getMismatchFilename().length() > 0)
                                    {
                                        assert mismatchFile != null;
                                        mismatchFile.println(item.getFullPath());
                                    }

                                    /* If the group is switching, process the current one. */
                                    if (isNewGrouping(item))
                                    {
                                        logger.info("Switching groups from '" + lastGroupName + "' to '" + currentGroupName + "'");
                                        // There is a new group - process the old group
                                        copyGroup(group, totalSize, false);
                                        totalSize = 0L;

                                        // Flush the output files
                                        if (cfg.getWhatsNewFilename().length() > 0)
                                        {
                                            whatsNewFile.flush();
                                        }
                                        if (cfg.getMismatchFilename().length() > 0)
                                        {
                                            mismatchFile.flush();
                                        }
                                    }
                                    long size = 0L;
                                    if (scanned || item.getSize() < 0)
                                    {
                                        size = getItemSize(item);
                                        item.setSize(size);
                                        totalSize += size;
                                    }
                                    group.add(item);
                                }
                            }
                        }
                    }
                }
                else
                {
                    throw new MungerException("Subscribed Publisher library " + subLib.name + " not found");
                }
            }
        }
        catch (Exception e)
        {
            logger.error("Exception " + e.getMessage() + " trace: " + Utils.getStackTrace(e));
        }
        finally
        {
            if (group.size() > 0)
            {
                // Process the last group
                logger.info("Processing last group '" + currentGroupName + "'");
                copyGroup(group, totalSize, false);
                totalSize = 0L;
            }

            // Close all the files and show the results
            if (mismatchFile != null)
            {
                mismatchFile.println("----------------------------------------------------");
                mismatchFile.println("Total items: " + grandTotalItems);
                mismatchFile.println("Total size : " + Utils.formatLong(grandTotalSize));
                mismatchFile.close();
            }
            if (whatsNewFile != null)
            {
                whatsNewFile.println("--------------------------------");
                whatsNewFile.println("Total for " + currLib + " = " + whatsNewTotal);
                whatsNewFile.println("================================");
                whatsNewFile.close();
            }
        }

        logger.info("-----------------------------------------------------");
        if (ignoredList.size() > 0)
        {
            logger.info("Ignored " + ignoredList.size() + " files: ");
            for (String s : ignoredList)
            {
                logger.info("    " + s);
            }
        }
        logger.info("Total copies: " + copyCount + ((!cfg.isDryRun()) ? ", of those " + grandTotalOriginalLocation + " went to original locations" : ""));
        logger.info("Total errors: " + errorCount);
        logger.info("Total items: " + grandTotalItems);
        logger.info("Total size : " + Utils.formatLong(grandTotalSize));
    }

    /**
     * Process everything
     * <p>
     * This is the where a munge run starts and ends based on configuration.
     * <p>
     * What is done depends on the combination of options specified on the command line.
     */
    public int process()
    {
        boolean noTargs = false;
        int returnValue = 0;

        try
        {
            try
            {
                // For -r P connect to remote subscriber -r S
                if (cfg.isRemotePublish())
                {
                    // sanity checks
                    if (context.publisherRepo.getLibraryData().libraries.flavor == null ||
                            context.publisherRepo.getLibraryData().libraries.flavor.length() < 1)
                    {
                        throw new MungerException("Publisher data incomplete, missing 'flavor'");
                    }

                    if (context.subscriberRepo.getLibraryData().libraries.flavor == null ||
                            context.subscriberRepo.getLibraryData().libraries.flavor.length() < 1)
                    {
                        throw new MungerException("Subscriber data incomplete, missing 'flavor'");
                    }

                    // check for opening commands from Subscriber
                    // *** might change cfg options for subscriber and targets that are handled below ***
                    if (context.clientStty.checkBannerCommands())
                    {
                        logger.info("Received subscriber commands:" + (cfg.isRequestCollection() ? " RequestCollection " : "") + (cfg.isRequestTargets() ? "RequestTargets" : ""));
                    }
                }

                // get -s Subscriber libraries
                if (cfg.getSubscriberLibrariesFileName().length() > 0)
                {
                    if (cfg.isRemoteSession() && cfg.isRequestCollection())
                    {
                        // request complete collection data from remote subscriber
                        String location = context.clientStty.retrieveRemoteData(cfg.getSubscriberLibrariesFileName(), "collection");
                        if (location == null || location.length() < 1)
                            throw new MungerException("Could not retrieve remote collections file");
                        cfg.setSubscriberLibrariesFileName(""); // clear so the collection file will be used
                        cfg.setSubscriberCollectionFilename(location);

                        context.subscriberRepo.read(cfg.getSubscriberCollectionFilename());
                    }
                }

                // process -e export text, publisher only
                if (cfg.getExportTextFilename().length() > 0)
                {
                    exportText();
                }

                // process -i export collection items, publisher only
                if (cfg.getExportCollectionFilename().length() > 0)
                {
                    exportCollection();
                }

                // get -t|T Targets
                if (cfg.getTargetsFilename().length() > 0)
                {
                    getStorageTargets();
                }
                else
                {
                    if (!cfg.isDryRun())
                    {
                        cfg.setDryRun(true);
                        noTargs = true;
                    }
                }

                // if all the pieces are specified munge the collections
                if ((cfg.getPublisherLibrariesFileName().length() > 0 ||
                        cfg.getPublisherCollectionFilename().length() > 0) &&
                        (cfg.getSubscriberLibrariesFileName().length() > 0 ||
                                cfg.getSubscriberCollectionFilename().length() > 0) &&
                        cfg.getTargetsFilename().length() > 0)
                {
                    if (noTargs)
                        logger.warn("NOTE: No targets file was specified - performing -D a dry run");

                    munge(); // this is the full munge process
                }
            }
            catch (Exception ex)
            {
                logger.error(ex.getMessage() + " toString=" + ex.toString());
                returnValue = 2;
            }
        }
        catch (Exception e)
        {
            logger.error(e.getMessage());
            returnValue = 1;
            cfg = null;
        }
        finally
        {
            // the - makes searching for the ending of a run easier
            if (logger != null)
            {
                logger.info("- Process end" + " ------------------------------------------");

                String resp = context.clientStty.roundTrip("quit");
                if (resp != null && !resp.equalsIgnoreCase("End-Execution"))
                {
                    logger.warn("Remote subscriber might not have quit");
                }
                //LogManager.shutdown();
            }
        }

        return returnValue;
    } // process

    public void resetCopyCount()
    {
        copyCount = 0;
    }

} // Process
