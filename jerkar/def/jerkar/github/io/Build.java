package jerkar.github.io;

import org.jerkar.api.file.JkPathTree;
import org.jerkar.api.java.JkJavaProcess;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsString;
import org.jerkar.api.utils.JkUtilsTime;
import org.jerkar.tool.JkDoc;
import org.jerkar.tool.JkInit;
import org.jerkar.tool.JkRun;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

class Build extends JkRun {

    JkPathTree jbakeSrc = getBaseTree().goTo("src/jbake");

    JkPathTree jbakeToolDir = getBaseTree().goTo("jerkar/tools/jbake-2.3.2");

    Path temp = getOutputDir().resolve("temp");

    Path targetSiteDir = getOutputDir().resolve("site");

    Path jerkarProjectPath = getBaseTree().getRoot().getParent().resolve("jerkar/org.jerkar.core");

    public static void main(String[] args) throws IOException {
        JkInit.instanceOf(Build.class, args).full();
    }

    @JkDoc({ "Generates the site and imports documentation inside.",
            "You must have the Jerkar repo (containing the documentation) in your git home." })
    public void full() throws IOException {
        clean();
        makeJbakeTemp();
        addJbakeHeaders();
        jbake();
        copyJerkarDoc();
        copyCurrentDist();
        copyCurrentJavadoc();
    }

    void makeJbakeTemp() {
        jbakeSrc.copyTo(temp);
    }

    void addJbakeHeaders() throws IOException {
        for (Path file : JkPathTree.of(temp).goTo("content").andMatching("*.md").getFiles()) {
            String content = jbakeHeader(file);
            byte[] previousContent = Files.readAllBytes(file);
            content = content + new String(previousContent, Charset.forName("UTF8"));
            Files.write(file, (content.getBytes(Charset.forName("UTF8"))), StandardOpenOption.CREATE);
        }
    }

    void copyJerkarDoc() {
        JkPathTree docTree = JkPathTree.of(jerkarProjectPath.resolve("jerkar/output/distrib/doc"));
        docTree.copyTo(targetSiteDir.resolve("doc"));
    }

    void copyCurrentDist() {
        Path siteDistDir = targetSiteDir.resolve("binaries");
        Path jerkarDistZip =jerkarProjectPath.resolve("jerkar/output/org.jerkar.core-distrib.zip");
        JkPathTree.of(siteDistDir).bring(jerkarDistZip, StandardCopyOption.REPLACE_EXISTING);
    }

    void copyCurrentJavadoc() {
        JkPathTree jerkarDistJavadoc = JkPathTree.of(jerkarProjectPath).goTo("org.jerkar.core/jerkar/output/javadoc");
        if (jerkarDistJavadoc.exists()) {
            jerkarDistJavadoc.copyTo(targetSiteDir.resolve("javadoc"));
        } else {
            JkLog.warn("Javadoc not found.");
        }
    }

    void jbake() {
        JkJavaProcess.of().withClasspath(jbakeToolDir.andMatching("lib/*.jar").getFiles())
                .runJarSync(jbakeToolDir.get("jbake-core.jar"), "jerkar/output/temp", "jerkar/output/site");
    }

    static String jbakeHeader(Path file) {
        String title = JkUtilsString.substringBeforeLast(file.getFileName().toString(), ".md");
        title = title.replace("_", " ").replace("-", " ");
        String template = "title=%s\ndate=%s\ntype=page\nstatus=published\n~~~~~~\n\n";
        return String.format(template, title, JkUtilsTime.now("yyyy-MM-dd"));
    }

}
