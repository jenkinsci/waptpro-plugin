//   Copyright 2016 Softlogica inc.
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.

package waptpro;

import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.AbstractItem;
import hudson.model.AbstractProject;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Action;
import hudson.model.DirectoryBrowserSupport;
import hudson.model.ProminentProjectAction;
import hudson.model.Run;
import hudson.model.Descriptor;
import hudson.Extension;


import java.io.File;
import java.io.IOException;

import javax.servlet.ServletException;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.DataBoundConstructor;

public class WaptProTarget extends AbstractDescribableImpl<WaptProTarget> 
{
    private final String reportName = "WAPT Pro Report";
    private final String reportsFolder;
    private final String reportFiles;
    private final boolean checkTestResult;
    private final String wrapperName = "index.html";

    public WaptProTarget(String reportsFolder, String reportFiles, boolean checkTestResult) 
    {
        this.reportsFolder = reportsFolder;
        this.reportFiles = reportFiles;
        this.checkTestResult = checkTestResult;
    }

    public String getReportName() 
    {
        return this.reportName;
    }

    public String getReportsFolder() 
    {
        return this.reportsFolder;
    }
    
    public String getReportFiles() 
    {
        return this.reportFiles;
    }

    public boolean getCheckTestResult() 
    {
           return this.checkTestResult;
    }

    public String getSanitizedName() 
    {
        String safeName = this.reportsFolder;
        safeName = safeName.replace(" ", "_");
        return safeName;
    }

    public String getWrapperName() 
    {
        return this.wrapperName;
    }

    public FilePath getArchiveTarget(AbstractBuild build) 
    {
        return new FilePath(getBuildArchiveDir(build));
    }

    private File getProjectArchiveDir(AbstractItem project) 
    {
        return new File(new File(project.getRootDir(), ""), this.getSanitizedName());
    }
    /**
     * Gets the directory where the HTML report is stored for the given build.
     */
    private File getBuildArchiveDir(Run run) 
    {
        return new File(new File(run.getRootDir(), "archive"), this.getSanitizedName());
    }

    protected abstract class BaseWaptAction implements Action 
    {
        private WaptProTarget actualWaptProTarget;

        public BaseWaptAction(WaptProTarget actualWaptProTarget) 
        {
            this.actualWaptProTarget = actualWaptProTarget;
        }

        public String getUrlName() 
        {
            return actualWaptProTarget.getSanitizedName();
        }

        public String getDisplayName() 
        {
            String action = actualWaptProTarget.reportName;
            return dir().exists() ? action : null;
        }

        public String getIconFileName() 
        {
            return dir().exists() ? "/plugin/waptpro/waptpro.png" : null;
        }

        public void doDynamic(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException 
        {
            DirectoryBrowserSupport dbs = new DirectoryBrowserSupport(this, new FilePath(this.dir()), this.getTitle(), "/plugin/waptpro/waptpro.png", false);
            dbs.setIndexFileName(WaptProTarget.this.wrapperName); // Hudson >= 1.312
            dbs.generateResponse(req, rsp, this);
        }

        protected abstract String getTitle();

        protected abstract File dir();
    }

    public class WaptAction extends BaseWaptAction implements ProminentProjectAction 
    {
        private final AbstractItem project;

        public WaptAction(AbstractItem project, WaptProTarget actualWaptProTarget) 
        {
            super(actualWaptProTarget);
            this.project = project;
        }

        @Override
        protected File dir() 
        {
            if (this.project instanceof AbstractProject) 
            {
                AbstractProject abstractProject = (AbstractProject) this.project;

                Run run = abstractProject.getLastSuccessfulBuild();
                if (run != null) 
                {
                    File javadocDir = getBuildArchiveDir(run);

                    if (javadocDir.exists()) 
                    {
                        return javadocDir;
                    }
                }
            }

            return getProjectArchiveDir(this.project);
        }

        @Override
        protected String getTitle() 
        {
            return this.project.getDisplayName() + " html2";
        }
    }

    public class WaptBuildAction extends BaseWaptAction 
    {
        private final AbstractBuild<?, ?> build;

        public WaptBuildAction(AbstractBuild<?, ?> build, WaptProTarget actualWaptProTarget) 
        {
            super(actualWaptProTarget);
            this.build = build;
        }
        
        public final AbstractBuild<?,?> getOwner() 
        {
            return build;
        }

        @Override
        protected String getTitle() 
        {
            return this.build.getDisplayName() + " html3";
        }

        @Override
        protected File dir() 
        {
            return getBuildArchiveDir(this.build);
        }
    }

    public void handleAction(AbstractBuild<?, ?> build) 
    {
        build.addAction(new WaptBuildAction(build, this));
    }

    public Action getProjectAction(AbstractProject project) 
    {
        return new WaptAction(project, this);
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<WaptProTarget> 
    {
        public String getDisplayName() { return ""; }
    }
}