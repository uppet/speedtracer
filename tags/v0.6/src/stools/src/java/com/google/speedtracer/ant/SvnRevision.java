package com.google.speedtracer.ant;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNWCClient;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

import java.io.File;

/**
 * An ant task that will examine an svn working directory and construct a svn
 * branding string that includes the svn revision and a user specified suffix.
 */
public class SvnRevision extends Task {

  /**
   * Encapsulates an SVN working copy.
   */
  private static class WorkingCopy {
    private final File workingDirectory;

    private final SVNWCClient client;

    public WorkingCopy(File workingCopyDirectory) {
      this.workingDirectory = workingCopyDirectory;
      client = SVNClientManager.newInstance(
          SVNWCUtil.createDefaultOptions(false)).getWCClient();
    }

    public SVNRevision getRevision() throws SVNException {
      return client.doInfo(workingDirectory, SVNRevision.WORKING).getRevision();
    }
  }

  private String propertyName;

  private long defaultRevision = 0L;

  private File workingDirectory;

  /**
   * Enables the ant attribute 'workingDirectory'
   * 
   * @param workingDirectory
   */
  public void setWorkingDirectory(File workingDirectory) {
    this.workingDirectory = workingDirectory;
  }

  /**
   * Enables the ant attribute 'property'. The specfied property will be updated
   * with the svn branding string.
   * 
   * @param propertyName an ant property name
   */
  public void setProperty(String propertyName) {
    this.propertyName = propertyName;
  }

  /**
   * Enables the ant attribute 'defaultRevision'. The default revision is used
   * when svn info fails on the working directory.
   * 
   * @param defaultRevision a revision identifier to fallback on in case of
   *          failure.
   */
  public void setDefaultRevision(long defaultRevision) {
    this.defaultRevision = defaultRevision;
  }

  @Override
  public void execute() throws BuildException {
    final File directory = (workingDirectory == null)
        ? getProject().getBaseDir() : workingDirectory;
    validate(directory, propertyName);
    getProject().setNewProperty(propertyName,
        Long.toString(resolveRevisionInfo(directory, defaultRevision)));
  }

  private static void validate(File workingDirectory, String propertyName) {
    if (propertyName == null) {
      throw new BuildException("revisionProperty is a required attribute.");
    }

    if (!workingDirectory.isDirectory()) {
      throw new BuildException("workingDirectory must be a directory.");
    }
  }

  private static long resolveRevisionInfo(File workingDirectory,
      long defaultRevision) {
    try {
      return new WorkingCopy(workingDirectory).getRevision().getNumber();
    } catch (SVNException e) {
      return defaultRevision;
    }
  }
}
