package net.tkausl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Deploys the artifact to your development-server and reloads the server
 */
@Mojo( name = "deploy" )
@Execute( phase = LifecyclePhase.PACKAGE )
public class DeployMojo
    extends AbstractMojo
{
    /**
     * Path to your local development server
     */
    @Parameter( alias = "path", required = true, defaultValue = "${spigot.path}")
    private File serverPath;
    
    /**
     * Final deployed filename without the .jar fileending
     */
    @Parameter( alias = "name", defaultValue = "${project.artifactId}", required = true)
    private String fileName;
    
    @Parameter( defaultValue = "${executedProject}", readonly = true, required = true )
    private MavenProject project;

    public void execute()
        throws MojoExecutionException
    {
        this.getLog().info("Deploying plugin...");
        if(!serverPath.exists()) throw new MojoExecutionException ("Passed server-path doesn't exist");
        File pluginPath = new File(serverPath, "plugins");
        File updatePath = new File(serverPath, "plugins/update");
        if(!updatePath.exists()) throw new MojoExecutionException ("Could not find plugins/update directory in passed path");
        File updateFile = new File(pluginPath, fileName + ".jar");
        File filePath = new File(updatePath, fileName + ".jar");
        if(!updateFile.exists()){
            try {
                Files.copy(project.getArtifact().getFile().toPath(), updateFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ex) {
                throw new MojoExecutionException ("Error while copying new file...", ex);
            }        
        }else{
            if(filePath.exists()){
                this.getLog().info("File already exists. Deleting...");
                filePath.delete();
            }
            this.getLog().info("Copying new file...");
            try {
                Files.copy(project.getArtifact().getFile().toPath(), filePath.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ex) {
                throw new MojoExecutionException ("Error while copying new file...", ex);
            }
        }
        this.getLog().info("File deployed.");
        Properties prop = new Properties();
        try {
            prop.load(new FileInputStream(new File(serverPath, "server.properties")));
        } catch (IOException ex) {
            throw new MojoExecutionException ("No server.properties found", ex);
        }
        String rconEnabled = prop.getProperty("enable-rcon");
        if(rconEnabled == null || !rconEnabled.equalsIgnoreCase("true")){
            throw new MojoExecutionException ("RCon is not enabled. Please enable RCon in your server.properties");
        }
        String rconPassword = prop.getProperty("rcon.password");
        if(rconPassword == null || rconPassword.equals("")){
            throw new MojoExecutionException ("No RCon password set. Please set a RCon password in your server.properties");
        }
        String rconPort = prop.getProperty("rcon.port");
        int rconPortInt = 25575;
        if(rconPort == null){
            getLog().info("No RCon port set. Defaulting to 25575");
            
        }else{
            try {
                rconPortInt = Integer.parseInt(rconPort);
            }catch(Throwable t){
                getLog().warn("Could not parse RCon port '" + rconPort + "'. Defaulting to 25575");
            }
        }
        
        try{
            RconConnection con = new RconConnection("localhost", rconPortInt);
            if(!con.login(rconPassword)){
                throw new MojoExecutionException ("Could not login to RCON. Wrong Password?");
            }
            con.command("reload");
            this.getLog().info("Server reloaded");
        } catch (IOException ex) {
            throw new MojoExecutionException ("Could not contact server", ex);
        }
    }
}
