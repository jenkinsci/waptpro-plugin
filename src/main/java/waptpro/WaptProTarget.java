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
import hudson.Extension;
import hudson.model.*;
import jenkins.tasks.SimpleBuildStep;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.DataBoundConstructor;

public class WaptProTarget extends AbstractDescribableImpl<WaptProTarget> 
{
    private final static String reportName = "WAPT Pro Report";
    private final String reportsFolder;
    private final String reportFiles;
    private final boolean checkTestResult;
    private final static String wrapperName = "index.html";

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

    public FilePath getArchiveTarget(Run build) 
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

    public class WaptAction implements Action
    {
        private final Job<?, ?> project;
        public WaptProTarget actualWaptProTarget;       

        public WaptAction(Job<?, ?> project, WaptProTarget actualWaptProTarget)
        {
            this.actualWaptProTarget = actualWaptProTarget;
            this.project = project;
        }

        protected File dir() 
        {
            if (this.project instanceof Job) 
            {
                Job job = this.project;

                Run run = job.getLastSuccessfulBuild();
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

        protected String getTitle() 
        {
            return this.project.getDisplayName() + " html2";
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
            DirectoryBrowserSupport dbs = new DirectoryBrowserSupport(this, new FilePath(this.dir()), this.getTitle(), this.getIconFileName(), false);
            dbs.setIndexFileName(actualWaptProTarget.getReportFiles()); // Hudson >= 1.312
            dbs.generateResponse(req, rsp, this);
        }    
    }

    public class WaptBuildAction implements Action, SimpleBuildStep.LastBuildAction
    {
        private final Run<?, ?> build;
        private final List<WaptAction> projectActions;

        public WaptBuildAction(Run<?, ?> build, WaptProTarget actualWaptProTarget) 
        {       
            this.build = build;
       
            List<WaptAction> projectActions = new ArrayList<>();
            projectActions.add(new WaptAction(build.getParent(), actualWaptProTarget));
            this.projectActions = projectActions;
        }
        
        public final Run<?,?> getBuild() 
        {
            return build;
        }

        public String getUrlName() 
        {
            return projectActions.get(0).getUrlName();
        }

        public String getDisplayName() 
        {
            return projectActions.get(0).getDisplayName();
        }

        public String getIconFileName() 
        {
            return dir().exists() ? "/plugin/waptpro/waptpro.png" : null;
        }
        
        protected String getTitle() 
        {
            return this.build.getDisplayName();
        }

        protected File dir() 
        {
            return getBuildArchiveDir(this.build);
        }

        @Override
        public Collection<? extends Action> getProjectActions() {
            return this.projectActions;
        }        
                
        public void doDynamic(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException 
        {         
            DirectoryBrowserSupport dbs = new DirectoryBrowserSupport(this, new FilePath(this.dir()), this.getTitle(), this.getIconFileName(), false);
                    
            String reportFileName = projectActions.get(0).actualWaptProTarget.getReportFiles();
                    
            dbs.setIndexFileName(reportFileName); // Hudson >= 1.312       
            dbs.generateResponse(req, rsp, this);
        }  
    }

    public void handleAction(Run<?, ?> build) 
    {
        build.addAction(new WaptBuildAction(build, this));
    }

    public Action getProjectAction(Job project) 
    {
        return new WaptAction(project, this);
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<WaptProTarget> 
    {
        public String getDisplayName() { return ""; }
    }
}