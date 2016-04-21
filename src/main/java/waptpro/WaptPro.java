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

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.matrix.MatrixConfiguration;
import hudson.matrix.MatrixProject;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.servlet.ServletException;
import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class WaptPro extends Recorder {
    private final WaptProTarget reportTarget;

    @DataBoundConstructor
    public WaptPro(String reportsFolder, String reportFiles, boolean checkTestResult) {
        this.reportTarget = new WaptProTarget(reportsFolder, reportFiles, checkTestResult);
    }
    
    public WaptProTarget getReportTarget() {
        return this.reportTarget;
    }
    
    public String getReportFiles() {
        return this.reportTarget.getReportFiles();
    }

    public boolean getCheckTestResult() {
           return this.reportTarget.getCheckTestResult();
    }
    
    public String getReportsFolder() {
        return this.reportTarget.getReportsFolder();
    }
    
    private static void writeFile(ArrayList<String> lines, File path) throws IOException {
        final OutputStream is = new FileOutputStream(path);
        
        try{
            final Writer r = new OutputStreamWriter(is, "UTF-8");

            BufferedWriter bw = new BufferedWriter(r);
            for (int i = 0; i < lines.size(); i++) {
                bw.write(lines.get(i));
                bw.newLine();
            }
            bw.close();
        }finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public ArrayList<String> readFile(String filePath) throws java.io.FileNotFoundException,
            java.io.IOException {
        ArrayList<String> aList = new ArrayList<String>();

        try {
            final InputStream is = this.getClass().getResourceAsStream(filePath);
            try {
                final Reader r = new InputStreamReader(is, "UTF-8");
                try {
                    final BufferedReader br = new BufferedReader(r);
                    try {
                        String line = null;
                        while ((line = br.readLine()) != null) {
                            aList.add(line);
                        }
                        br.close();
                        r.close();
                        is.close();
                    } finally {
                        try {
                            br.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } finally {
                    try {
                        r.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } finally {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            // failure
            e.printStackTrace();
        }

        return aList;
    }

    protected static String resolveParametersInString(AbstractBuild<?, ?> build, BuildListener listener, String input) {
        try {
            return build.getEnvironment(listener).expand(input);
        } catch (Exception e) {
            listener.getLogger().println("Failed to resolve parameters in string \""+
            input+"\" due to following error:\n"+e.getMessage());
        }
        return input;
    }
    
    protected ArrayList<String> readLines(String filename) throws IOException 
    {  
        ArrayList<String> lines = new ArrayList<String>();
        
        try {
            final InputStream is = new FileInputStream(filename);
        
            try{
                final Reader r = new InputStreamReader(is, "UTF-8");

                BufferedReader bufferedReader = new BufferedReader(r);

                String line = null;
                while ((line = bufferedReader.readLine()) != null)
                {
                    lines.add(line);
                }
                bufferedReader.close();
            } finally {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            // failure
            e.printStackTrace();
        }
        
        return lines;
    }
    
    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException
    {
        listener.getLogger().println("WAPT Pro Plugin > Processing WAPT Pro output files...");
        
        // Grab the contents of the header and footer as arrays
        ArrayList<String> headerLines;
        ArrayList<String> footerLines;
        try 
        {
            headerLines = this.readFile("/waptpro/WaptPro/header.html");
            footerLines = this.readFile("/waptpro/WaptPro/footer.html");
        }
        catch (FileNotFoundException e1) 
        {
            e1.printStackTrace();
            return false;
        }
        catch (IOException e1) 
        {
            e1.printStackTrace();
            return false;
        }
        
        {
            // Create an array of lines we will eventually write out, initially the header.
            ArrayList<String> reportLines = new ArrayList<String>(headerLines);
            WaptProTarget reportTarget = getReportTarget(); 

            FilePath targetDir = reportTarget.getArchiveTarget(build);

             // The index name might be a comma separated list of names, so let's figure out all the pages we should index.
            String[] csvReports = resolveParametersInString(build, listener, reportTarget.getReportFiles()).split(",");
            ArrayList<String> reports = new ArrayList<String>();
            for (int j=0; j < csvReports.length; j++) 
            {
                String report = csvReports[j];
                report = report.trim();          

                // Ignore blank report names caused by trailing or double commas.
                if (report.equals("")) {continue;}

                if(!report.endsWith(".html"))
                {
                    report += ".html";
                }

                reports.add(report);

                reportLines.add("<iframe id=\"myframe\" src=\"" + report + "\" height=\"100%\" width=\"100%\" frameborder=\"0\"></iframe>");
               
                if(getCheckTestResult())
                {                
                    listener.getLogger().println("WAPT Pro Plugin > Check test result in " + targetDir + "\\" + report + " report file");
                                    
                    ArrayList<String> reportStrings;
                    try
                    {
                        reportStrings = readLines(targetDir + "\\" + report);
                    }
                    catch(IOException e)
                    {
                        Util.displayIOException(e, listener);
                        e.printStackTrace(listener.fatalError("WAPT Pro failure"));
                        build.setResult(Result.FAILURE);
                        return true;                    
                    }

                    boolean resultFound = false;
                    for(int reportStrNum=0; reportStrNum<reportStrings.size(); reportStrNum++)
                    {                  
                        if(reportStrings.get(reportStrNum).contains("meta name='TestResult' content='FAILURE'"))
                        {
                            listener.getLogger().println("WAPT Pro Plugin > Test result is FAILURE");
                            build.setResult(Result.FAILURE);  
                            resultFound = true;
                            break;
                        }                  
                        if(reportStrings.get(reportStrNum).contains("meta name='TestResult' content='SUCCESS'"))
                        {
                            listener.getLogger().println("WAPT Pro Plugin > Test result is SUCCESS");
                            build.setResult(Result.SUCCESS);
                            resultFound = true;
                            break;
                        }
                    }

                    if(!resultFound)
                    {
                        listener.getLogger().println("WAPT Pro Plugin > Test result was not found");
                        build.setResult(Result.FAILURE); 
                    }
                }
            }
 
//            String levelString = "BUILD"; 
//            listener.getLogger().println("WAPT Pro Plugin > Archiving at " + levelString + " level " + archiveDir + " to " + targetDir);
            
            // Add the JS to change the link as appropriate.
            String hudsonUrl = Hudson.getActiveInstance().getRootUrl();
            AbstractProject job = build.getProject();
            reportLines.add("<script type=\"text/javascript\">document.getElementById(\"hudson_link\").innerHTML=\"Back to " + job.getName() + "\";</script>");
            // If the URL isn't configured in Hudson, the best we can do is attempt to go Back.
            if (hudsonUrl == null) 
            {
                reportLines.add("<script type=\"text/javascript\">document.getElementById(\"hudson_link\").onclick = function() { history.go(-1); return false; };</script>");
            } 
            else 
            {
                String jobUrl = hudsonUrl + job.getUrl();
                reportLines.add("<script type=\"text/javascript\">document.getElementById(\"hudson_link\").href=\"" + jobUrl + "\";</script>");
            }

            // Now add the footer.
            reportLines.addAll(footerLines);
            // And write this as the index
            try 
            {
                reportTarget.handleAction(build);
                writeFile(reportLines, new File(targetDir.getRemote(), reportTarget.getWrapperName())); 
            }
            catch (IOException e) 
            {
                e.printStackTrace();
            }
        }

        return true;
    }

    @Override
    public Collection<? extends Action> getProjectActions(AbstractProject<?, ?> project) 
    {  
        ArrayList<Action> actions = new ArrayList<Action>();
        WaptProTarget target = this.reportTarget; 

        actions.add(target.getProjectAction(project));
        if (project instanceof MatrixProject && ((MatrixProject) project).getActiveConfigurations() != null){
            for (MatrixConfiguration mc : ((MatrixProject) project).getActiveConfigurations()){
                try {
                  mc.onLoad(mc.getParent(), mc.getName());
                }
                catch (IOException e){
                    //Could not reload the configuration.
                }
            }
        }

        return actions;
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        @Override
        public String getDisplayName() {
            // return Messages.JavadocArchiver_DisplayName();
            return "Publish WAPT Pro reports";
        }

        /**
         * Performs on-the-fly validation on the file mask wildcard.
         */
        public FormValidation doCheck(@AncestorInPath AbstractProject project,
                @QueryParameter String value) throws IOException, ServletException {
            return FormValidation.ok();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }
}
