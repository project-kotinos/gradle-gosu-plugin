package org.gosulang.gradle.tasks;

import org.gradle.api.Buildable;
import org.gradle.api.GradleException;
import org.gradle.api.Nullable;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency;
import org.gradle.api.internal.file.FileCollectionInternal;
import org.gradle.api.internal.file.collections.LazilyInitializedFileCollection;
import org.gradle.api.logging.Logger;
import org.gradle.api.tasks.TaskDependency;
import org.gradle.internal.Cast;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GosuRuntime {

  private static final Pattern GOSU_JAR_PATTERN = Pattern.compile("gosu-(\\w.*?)-(\\d.*).jar");
  private static final String LF = System.lineSeparator();

  private final Project _project;

  public GosuRuntime(Project project) {
    _project = project;
    _project.getLogger().quiet("Constructing Gosu Runtime");
  }


  /**
   * Searches the specified classpath for a 'gosu-core-api' Jar, and returns a classpath
   * containing a corresponding (same version) 'gosu-core' Jar and its dependencies.
   *
   * <p>The returned class path may be empty, or may fail to resolve when asked for its contents.
   *
   * @param classpath a classpath containing a 'gosu-core-api' Jar
   * @return a classpath containing a corresponding 'gosu-core' Jar and its dependencies
   */
  public FileCollection inferGosuClasspath(final Iterable<File> classpath) {

    return new LazilyInitializedFileCollection() {
      @Override
      public String getDisplayName() {
        return "Gosu runtime classpath";
      }

      @Override
      public FileCollectionInternal createDelegate() {
        Logger logger = _project.getLogger();

        logger.quiet("Evaluating GosuRuntime#inferGosuClasspatn$LazilyInitializedFileCollection#createDelegate()");

        if (_project.getRepositories().isEmpty()) {
          throw new GradleException("Cannot infer Gosu classpath because no repository is declared in " + _project);
        }



        File gosuCoreApiJar = findGosuJar(classpath, "core-api");
        logger.quiet("Got gosuCoreApiJar:" + gosuCoreApiJar.getAbsolutePath());
//        File gosuCoreJar = findGosuJar(classpath, "core");
//        Configuration runtime = _project.getConfigurations().getByName("runtime");
//        logger.quiet("Runtime convention isssss: " + runtime.getFiles());
//        File gosuCoreJar = findGosuJar(runtime, "core");
//        File gosuCoreJar = findGosuJar(_project.getConfigurations().getByName("runtime"), "core");

        //could not find Gosu as external dependencies; before throwing check if they are present on the classpath in another form
//        if(gosuCoreApiJar == null) {
//          logger.quiet("gosuCoreApiJar was null; going to Plan B");
//          gosuCoreApiJar = getClassLocation("gw.lang.Gosu");
//        }
//        if(gosuCoreJar == null) {
//          logger.quiet("gosuCoreJar was null; going to Plan B");
//          gosuCoreJar = getClassLocation("gw.internal.gosu.parser.MetaType");
//        }

//        if(gosuCoreApiJar == null || gosuCoreJar == null) {
//          throw new GradleException(String.format("Cannot infer Gosu class path because both the Gosu Core API and Gosu Core Jars were not found." + LF +
//              "Does %s declare dependency to gosu-core-api and gosu-core? Searched classpath: %s.", _project, classpath) + LF +
//              "An example dependencies closure may resemble the following:" + LF +
//              LF +
//              "dependencies {" + LF +
//              "    compile 'org.gosu-lang.gosu:gosu-core-api:1.8.1'" + LF +
//              "    runtime 'org.gosu-lang.gosu:gosu-core:1.8.1'" + LF +
//              "}" + LF);
//        }
        if(gosuCoreApiJar == null) {
          throw new GradleException(String.format("Cannot infer Gosu class path because the Gosu Core API Jar was not found." + LF +
              "Does %s declare dependency to gosu-core-api? Searched classpath: %s.", _project, classpath) + LF +
              "An example dependencies closure may resemble the following:" + LF +
              LF +
              "dependencies {" + LF +
              "    compile 'org.gosu-lang.gosu:gosu-core-api:1.8.1'" + LF +
              "}" + LF);
        }

        String gosuCoreApiVersion = getGosuVersion(gosuCoreApiJar);
//        String gosuCoreVersion = getGosuVersion(gosuCoreJar);

        if (gosuCoreApiVersion == null ) {
          throw new AssertionError(String.format("Unexpectedly failed to parse version of Gosu Jar file: %s in %s", gosuCoreApiJar, _project));
        }
//        if (gosuCoreVersion == null ) {
//          throw new AssertionError(String.format("Unexpectedly failed to parse version of Gosu Jar file: %s in %s", gosuCoreJar, _project));
//        }

//        if(!gosuCoreApiVersion.equals(gosuCoreVersion)) {
//          throw new GradleException("Gosu library version mismatch detected.  Please ensure these two libraries version numbers are in sync:" + LF
//              + "gosu-core-api:" + gosuCoreApiVersion + LF
//              + "gosu-core:" + gosuCoreVersion);
//        }

        return Cast.cast(FileCollectionInternal.class, _project.getConfigurations().detachedConfiguration(
//            new DefaultExternalModuleDependency("org.gosu-lang.gosu", "gosu-core-api", gosuCoreApiVersion),
            new DefaultExternalModuleDependency("org.gosu-lang.gosu", "gosu-core", gosuCoreApiVersion)));
      }

      // let's override this so that delegate isn't created at autowiring time (which would mean on every build)
      @Override
      public TaskDependency getBuildDependencies() {
        if (classpath instanceof Buildable) {
          return ((Buildable) classpath).getBuildDependencies();
        }
        return task -> Collections.emptySet();
      }
    };

  }

  /**
   * Searches the specified class path for a Gosu Jar file (gosu-core, gosu-core-api, etc.) with the specified appendix (compiler, library, jdbc, etc.).
   * If no such file is found, {@code null} is returned.
   *
   * @param classpath the class path to search
   * @param appendix the appendix to search for
   * @return a Gosu Jar file with the specified appendix
   */
  @Nullable
  public File findGosuJar(Iterable<File> classpath, String appendix) {
    for (File file : classpath) {
      Matcher matcher = GOSU_JAR_PATTERN.matcher(file.getName());
      if (matcher.matches() && matcher.group(1).equals(appendix)) {
        return file;
      }
    }
    return null;
  }

  private File getClassLocation(String className) {
    Class clazz;
    try {
      clazz = Class.forName(className);
    } catch (ClassNotFoundException e) {
      return null;
    }
    ProtectionDomain pDomain = clazz.getProtectionDomain();
    CodeSource cSource = pDomain.getCodeSource();
    if (cSource != null) {
      URL loc = cSource.getLocation();
      File file;
      try {
        file = new File(URLDecoder.decode(loc.getPath(), "UTF-8"));
      } catch (UnsupportedEncodingException e) {
        _project.getLogger().warn("Unsupported Encoding for URL: " + loc, e);
        file = new File(loc.getPath());
      }
      _project.getLogger().info("Found location <" + file.getPath() + "> for className <" + className + ">");
      return file;
    } else {
      return null;
    }
  }

  /**
   * Determines the version of a Gosu Jar file (gosu-core, gosu-core-api, etc.). 
   * If the version cannot be determined, or the file is not a Gosu
   * Jar file, {@code null} is returned.
   *
   * <p>Implementation note: The version is determined by parsing the file name, which
   * is expected to match the pattern 'gosu-[component]-[version].jar'.
   *
   * @param gosuJar a Gosu Jar file
   * @return the version of the Gosu Jar file
   */
  @Nullable
  public String getGosuVersion( File gosuJar ) {
    Matcher matcher = GOSU_JAR_PATTERN.matcher(gosuJar.getName());
    return matcher.matches() ? matcher.group(2) : null;
  }

}