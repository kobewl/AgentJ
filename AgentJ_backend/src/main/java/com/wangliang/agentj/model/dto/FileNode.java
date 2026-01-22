package com.wangliang.agentj.model.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * File tree node representation
 * Used for representing file system structure in file browser API
 */
public class FileNode {

    private String name;

    private String path;

    private String type; // "file" or "directory"

    private long size;

    private String lastModified;

    private List<FileNode> children;

    public FileNode() {
    }

    public FileNode(String name, String path, String type, long size, String lastModified) {
        this.name = name;
        this.path = path;
        this.type = type;
        this.size = size;
        this.lastModified = lastModified;
        this.children = new ArrayList<>();
    }

    // Getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getLastModified() {
        return lastModified;
    }

    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }

    public List<FileNode> getChildren() {
        return children;
    }

    public void setChildren(List<FileNode> children) {
        this.children = children;
    }

}
