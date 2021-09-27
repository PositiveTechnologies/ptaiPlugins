package com.ptsecurity.appsec.ai.ee.utils.ci.integration.ptaiserver.utils;

import com.ptsecurity.appsec.ai.ee.ptai.server.ApiException;
import com.ptsecurity.appsec.ai.ee.ptai.server.ApiHelper;
import com.ptsecurity.appsec.ai.ee.utils.ci.integration.base.Base;
import com.ptsecurity.appsec.ai.ee.utils.ci.integration.ptaiserver.domain.Transfer;
import com.ptsecurity.appsec.ai.ee.utils.ci.integration.ptaiserver.domain.Transfers;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.apache.commons.compress.archivers.ArchiveStreamFactory.ZIP;
import static org.joor.Reflect.on;

@RequiredArgsConstructor
public class FileCollector {

    /**
     * Need to get scanned directories list for debugging purposes, but
     * getScannedDirs is package-private method, so we need to use
     * reflection
     * @param ds Directory scanner that was used for file scanning
     * @return Array of scanned directories names
     */
    private String[] getScannedDirs(DirectoryScanner ds) {
        Set<String> res = on(ds).call ("getScannedDirs").get();
        return res.toArray(new String[0]);
    }

    @AllArgsConstructor
    @Getter
    static class FileEntry {
        private final String fileName;
        private final String name;
    }

    private final Transfers transfers;
    private final Base owner;

    public void collect(@NonNull final File dir, @NonNull final File zip) throws ApiException {
        List<FileEntry> fileEntries = collectFiles(dir);
        ApiHelper.callApi(
                () -> packCollectedFiles(zip, fileEntries),
                "Collected files pack error");
    }

    public static File collect(Transfers transfers, final File dir, @NonNull Base owner) throws ApiException {
        File zip = createTempFile();
        return collect(transfers, dir, zip, owner);
    }

    public static File createTempFile() throws ApiException {
        return ApiHelper.callApi(
                () -> File.createTempFile("ptai.", ".zip"),
                "Temp file create error");
    }

    public static File collect(Transfers transfers, @NonNull final File dir, @NonNull final File zip, @NonNull Base owner) throws ApiException {
        try {
            owner.fine("Create file collector");
            FileCollector collector = new FileCollector(transfers, owner);

            if (!dir.exists() || !dir.canRead()) {
                String reason = "Unknown problem with source folder " + dir.getAbsolutePath();
                if (!dir.exists())
                    reason = "Source folder " + dir.getAbsolutePath() + " does not exist";
                else if (!dir.canRead())
                    reason = "Source folder " + dir.getAbsolutePath() + " can not be read";
                throw ApiException.raise("File collect failed", new IllegalArgumentException(reason));
            } else
                owner.info("Folder to collect files from is %s", dir.getAbsolutePath());
            owner.info("Sources will be zipped to %s", zip.getAbsolutePath());
            List<FileCollector.FileEntry> fileEntries = collector.collectFiles(dir);
            if (fileEntries.isEmpty())
                throw new IllegalArgumentException("No files are match defined transfer settings");
            collector.packCollectedFiles(zip, fileEntries);
            owner.info("Zipped sources size is %s (%d bytes)", bytesToString(zip.length()), zip.length());
            return zip;
        } catch (Exception e) {
            throw ApiException.raise("File collect failed", e);
        }
    }

    private static final int MAX_DETAILS = 20;

    private void verboseCollectionDetails(String[] items, String prefix) {
        if (null == owner) return;
        if (null == items || 0 == items.length)
            verbose("=== %s list is empty ===", prefix);
        else {
            verbose("=== %s [%d] list begin ===", prefix, items.length);
            int total = items.length < MAX_DETAILS ? items.length : MAX_DETAILS;
            int pre = total >> 1;
            int post = total - pre;
            for (int i = 0 ; i < pre ; i++)
                verbose("%d: %s", i, items[i]);
            if (items.length != pre + post)
                verbose("... Skipping %d entries ...", items.length - pre - post);
            for (int i = items.length - post ; i < items.length ; i++)
                verbose("%d: %s", i, items[i]);
            verbose("==== %s [%d] list end ====", prefix, items.length);
        }
    }

    protected void verbose(String format, Object ... data) {
        if (null != owner) owner.fine(format, data);
    }

    public List<FileEntry> collectFiles(@NonNull final File dir) throws ApiException {
        verbose("collectFiles called for %s", dir.getAbsolutePath());
        List<FileEntry> res = new ArrayList<>();
        for (Transfer transfer : this.transfers) {
            // Normalize prefix
            String removePrefix = Optional.ofNullable(
                    FilenameUtils.separatorsToUnix(
                            FilenameUtils.normalize(transfer.getRemovePrefix() + "/")))
                    .orElse("");
            if ('/' == removePrefix.charAt(0))
                removePrefix = removePrefix.substring(1);
            verbose("Pattern separator = %s", transfer.getPatternSeparator().isEmpty() ? "[empty]" : transfer.getPatternSeparator());
            verbose("Remove prefix = %s", removePrefix.isEmpty() ? "[empty]" : removePrefix);
            verbose("Includes = %s", transfer.getIncludes().isEmpty() ? "[empty]" : transfer.getIncludes());
            verbose("Use default excludes = %s", transfer.isUseDefaultExcludes());

            final FileSet fileSet = new FileSet();
            if (dir.isDirectory())
                fileSet.setDir(dir);
            else
                fileSet.setFile(dir);
            fileSet.setProject(new Project());
            if (null != transfer.getIncludes())
                for (String pattern : transfer.getIncludes().split(transfer.getPatternSeparator())) {
                    fileSet.createInclude().setName(pattern);
                    verbose("Include pattern = %s", pattern);
                }
            verbose("Excludes = %s", transfer.getExcludes().isEmpty() ? "[empty]" : transfer.getExcludes());
            if (null != transfer.getExcludes())
                for (String pattern : transfer.getExcludes().split(transfer.getPatternSeparator())) {
                    fileSet.createExclude().setName(pattern);
                    verbose("Exclude pattern = %s", pattern);
                }
            fileSet.setDefaultexcludes(transfer.isUseDefaultExcludes());
            String[] files = fileSet.getDirectoryScanner().getIncludedFiles();
            verboseCollectionDetails(files, "Included files");
            verboseCollectionDetails(getScannedDirs(fileSet.getDirectoryScanner()), "Scanned dirs");
            verboseCollectionDetails(fileSet.getDirectoryScanner().getNotIncludedFiles(), "Not included files");
            verboseCollectionDetails(fileSet.getDirectoryScanner().getDeselectedFiles(), "Deselected files");
            verboseCollectionDetails(fileSet.getDirectoryScanner().getExcludedFiles(), "Excluded files");
            // files is an array of this.srcDir - relative paths to files
            Path parentFolder = dir.isDirectory() ? dir.toPath() : dir.getParentFile().toPath();
            for (String file : files) {
                // Normalize relative path
                Path filePath = parentFolder.resolve(file);
                String relativePath = filePath.toUri().normalize().getPath();
                relativePath = StringUtils.removeStart(relativePath, parentFolder.toUri().normalize().getPath());
                String entryName;
                if (transfer.isFlatten())
                    entryName = filePath.getFileName().toString();
                else {
                    if (!relativePath.startsWith(removePrefix))
                        throw ApiException.raise("File collect failed", new IllegalArgumentException(String.format("File's %s does not starts with prefix %s", file, removePrefix)));
                    entryName = StringUtils.removeStart(relativePath, removePrefix);
                }
                verbose("File %s will be added as %s", filePath.toString(), entryName);
                res.add(new FileEntry(filePath.toString(), entryName));
            }
        }
        return res;
    }

    /*
    There's no need to create multipart Zip-archive during this stage as technically such an archive
    is a single-part archive splitted after creation. That may be checked by opening zip parts starting
    from 2: those files does not contain any headers.
    So if multipart file upload will be implemented, than it is easier to create single-part archive
    and "split" it immediately during upload
    This also means that there's no need to use Zip4J library: it does support multipart archives
    but doesn't allow us to use custom file names as custom file name may be passed only by
    ZipParameter.setFileNameInZip, but there's no way to pass array of ZipParameters into createSplitZipFile
    method.
     */
    private void packCollectedFiles(@NonNull final File zip, final List<FileEntry> files) throws IOException, ArchiveException {
        verbose("Pack collected files to %s", zip.getAbsolutePath());
        File destDir = zip.getParentFile();

        if (!destDir.exists()) {
            verbose("Destination folder %s doesn't exist, creating", destDir.getAbsolutePath());
            destDir.mkdirs();
        }
        OutputStream zfs = new FileOutputStream(zip);
        ArchiveOutputStream as = new ArchiveStreamFactory().createArchiveOutputStream(ZIP, zfs);
        verbose("Zip stream created");

        for (FileEntry entry : files) {
            verbose("Add %s file as %s to zip stream", entry.fileName, entry.name);
            // Check if this is symlink with missing destination
            if (Files.isSymbolicLink(Paths.get(entry.fileName))) {
                verbose("%s is a symbolic link, let's check if its destination exist", entry.fileName);
                if (!Files.readSymbolicLink(Paths.get(entry.fileName)).toFile().exists()) {
                    verbose("Skip %s as there's no target file exist", entry.fileName);
                    continue;
                }
            }

            as.putArchiveEntry(new ZipArchiveEntry(entry.name));

            BufferedInputStream is = new BufferedInputStream(new FileInputStream(entry.fileName));
            int size = IOUtils.copy(is, as);
            verbose("%s zipped", bytesToString(size));
            is.close();
            as.closeArchiveEntry();
            verbose("File %s added as %s", entry.fileName, entry.name);
        }
        verbose("Closing zip stream");
        as.finish();
        zfs.close();
    }

    private static final double LOG1024 = Math.log10(1024);

    static String bytesToString(long byteCount) {
        String[] suf = new String[]{ "B", "KB", "MB", "GB", "TB", "PB", "EB" }; // Longs run out around EB
        if (0 == byteCount) return "0 " + suf[0];
        long bytes = Math.abs(byteCount);
        int idx = (int)(Math.floor(Math.log10(bytes) / LOG1024));
        double num = bytes / Math.pow(1024, idx);
        return (byteCount < 0 ? "-" : "") + new DecimalFormat("#.##").format(num) + " " + suf[idx];
    }

    public static String[] defaultExcludes() {
        return DirectoryScanner.getDefaultExcludes();
    }
}
