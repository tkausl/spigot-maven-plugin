/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.tkausl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import net.tkausl.pump.InputStreamPumper;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteStreamHandler;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 *
 * @author Tobias
 */
@Mojo( name = "run" )
@Execute( phase = LifecyclePhase.COMPILE )
public class RunMojo extends AbstractMojo {
    @Parameter( defaultValue = "${executedProject}", readonly = true, required = true )
    private MavenProject project;
    
    /**
     * Path to your local development server
     */
    @Parameter(alias = "path", required = true, defaultValue = "${spigot.path}")
    private File serverPath;

    /**
     * Path to your local development server
     */
    @Parameter(alias = "jar", required = true, defaultValue = "${spigot.jar}")
    private String serverJar;
    
    /**
     * Plugin Folder
     */
    @Parameter(alias = "pluginfolder")
    private String pluginfolder;
    
    /**
     * Final deployed filename without a fileending
     */
    @Parameter( alias = "name", defaultValue = "${project.artifactId}", required = true)
    private String fileName;
    
    
    @Parameter( readonly = true, property = "project.build.outputDirectory" )
    private File outputDir;
    
    
    public void execute() throws MojoExecutionException, MojoFailureException {
        File plugins = pluginfolder == null ? new File(serverPath, "plugins") : new File(serverPath, pluginfolder);
        if(!plugins.exists())
            plugins.mkdirs();
        
        
        File pluginFile = new File(plugins, fileName + ".dev");
        if(!pluginFile.exists()){
            try {
                PrintWriter w = new PrintWriter(pluginFile);
                w.print(outputDir);
                w.close();
            } catch (FileNotFoundException ex) {
                throw new MojoExecutionException("Could not write .dev-file");
            }
        }
        Executor ex = new DefaultExecutor();
        CommandLine commandLine = CommandLine.parse("java");
        String execArgs = System.getProperty("exec.args");
        if(execArgs != null && execArgs.trim().length() > 0){
            commandLine.addArguments(execArgs.split(" "));
        }
        commandLine.addArguments(new String[]{"-jar", serverJar});
        
        if(pluginfolder != null)
            commandLine.addArguments(new String[]{"--plugins", pluginfolder});
        
        ex.setWorkingDirectory(serverPath);
        //PumpStreamHandler psh = new PumpStreamHandler(System.out, System.err, System.in);
        //ex.setStreamHandler(psh);
        ex.setStreamHandler(new ExecuteStreamHandler() {
            private PumpStreamHandler psh = new PumpStreamHandler(System.out, System.err);
            InputStreamPumper isp;
            Thread ispt;
            public void setProcessInputStream(OutputStream os) throws IOException {
                isp = new InputStreamPumper(System.in, os);
            }

            public void setProcessErrorStream(InputStream is) throws IOException {
                psh.setProcessErrorStream(is);
            }

            public void setProcessOutputStream(InputStream is) throws IOException {
                psh.setProcessOutputStream(is);
            }

            public void start() throws IOException {
                if(isp != null){
                    ispt = new Thread(isp);
                    ispt.setDaemon(true);
                    ispt.start();
                }
                psh.start();
            }

            public void stop() throws IOException {
                if(ispt != null)
                    ispt.interrupt();
                psh.stop();
            }
        });
        try {
            ex.execute(commandLine);
        } catch (IOException ex1) {
            throw new MojoExecutionException("Error in Execution");
        }
    }
}
