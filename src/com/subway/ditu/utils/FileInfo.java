/**
 * FileInfo.java
 */
package com.subway.ditu.utils;


/**
 * @author Guoqing Sun Oct 24, 201211:16:36 AM
 */
public class FileInfo {

    public String fileName;

    public String filePath;

    public long fileSize;

    public boolean isDir;

    public int count;

    public long modifiedDate;

    public boolean selected;

    public boolean canRead;

    public boolean canWrite;

    public boolean isHidden;

    public long dbId; // id in the database, if is from database

    @Override
    public String toString() {
        return "FileInfo [fileName=" + fileName + ", filePath=" + filePath + ", fileSize=" + fileSize + ", isDir="
                + isDir + ", count=" + count + ", modifiedDate=" + modifiedDate + ", selected=" + selected
                + ", canRead=" + canRead + ", canWrite=" + canWrite + ", isHidden=" + isHidden + ", dbId=" + dbId + "]";
    }
    
}
