/*
 * TACO: Translation of Annotated COde
 * Copyright (c) 2010 Universidad de Buenos Aires
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA,
 * 02110-1301, USA
 */
package ar.edu.taco;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Pattern;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.multijava.mjc.JCompilationUnitType;

import ar.edu.jdynalloy.JDynAlloyConfig;
import ar.edu.jdynalloy.MethodToCheckNotFoundException;
import ar.edu.jdynalloy.ast.JDynAlloyModule;
import ar.edu.taco.engine.AlloyStage;
import ar.edu.taco.engine.DynalloyStage;
import ar.edu.taco.engine.JDynAlloyParsingStage;
import ar.edu.taco.engine.JDynAlloyPrinterStage;
import ar.edu.taco.engine.JDynAlloyStage;
import ar.edu.taco.engine.JavaTraceStage;
import ar.edu.taco.engine.JmlStage;
import ar.edu.taco.engine.PrecompiledModules;
import ar.edu.taco.engine.SimpleJmlStage;
import ar.edu.taco.jfsl.JfslStage;
import ar.edu.taco.jml.JmlToSimpleJmlContext;
import ar.edu.taco.jml.parser.JmlParser;
import ar.edu.taco.simplejml.SimpleJmlToJDynAlloyContext;
import ar.edu.taco.utils.FileUtils;
import ar.uba.dc.rfm.alloy.AlloyTyping;
import ar.uba.dc.rfm.alloy.ast.formulas.AlloyFormula;
import ar.uba.dc.rfm.dynalloy.DynAlloyCompiler;
import ar.uba.dc.rfm.dynalloy.analyzer.AlloyAnalysisResult;
import ar.uba.dc.rfm.dynalloy.ast.DynalloyModule;
import ar.uba.dc.rfm.dynalloy.ast.ProgramDeclaration;

/**
 * <p>Runs the TACO analysis.</p>
 * <p>The configuration options must be stated through the configuration file
 * whose name expects the methods <code>ar.edu.taco.TacoMain.run</code>.
 * Those configurations can be overridden by the sencond argument of
 * <code>ar.edu.taco.TacoMain.run(String, Properties)</code>.</p>
 * <h3>Integers</h3>
 * <p>TACO can analyse code using Alloy integers or Java-like Integers.
 * In either case, the meaning of the bitwidth value is the same: a bound
 * in the count of numbers TACO will deal with. In particular, it states that
 * the range of integers used in the analysis include from -2^{bitwidth-1}
 * to 2^{bitwidth-1}-1.</p>
 * <p>Besides that, TACO can try to infer the value of the scopes to be used
 * for the analysis. If the bitwidth is setted to a non positive integer
 * <b>and</b> the scope inferring feature is activated, the bitwidth is also
 * inferred. Otherwise, the bitwidth value setted is used.</p>
 *
 * @author unknown (jgaleotti?)
 *
 */
public class TacoMain {


    private static final String CMD = "Taco";
    private static final String HEADER = "Taco static analysis tool.";
    private static final String FOOTER = "For questions and comments please write to jgaleotti AT dc DOT uba DOT ar";
    public static final String PATH_SEP = System.getProperty("path.separator");
    public static final String FILE_SEP = System.getProperty("file.separator");

    public static final String OUTPUT_SIMPLIFIED_JAVA_EXTENSION = ".java";

    private Object inputToFix;

    /**
     * @param args
     */
    @SuppressWarnings({ "static-access" })
    public static void main(String[] args) {
        @SuppressWarnings("unused")
        int loopUnrolling = 3;

        String tacoVersion = getManifestAttribute(Attributes.Name.IMPLEMENTATION_VERSION);
        String tacoCreatedBy = getManifestAttribute(new Name("Created-By"));

        System.out.println("TACO: Taco static analysis tool.");
        System.out.println("Created By: " + tacoCreatedBy);
        System.out.println("Version: " + tacoVersion);
        System.out.println("");
        System.out.println("");

        Option helpOption = new Option("h", "help", false, "print this message");
        Option versionOption = new Option("v", "version", false, "shows version");

        Option configFileOption = OptionBuilder.withArgName("path").withLongOpt("configFile").hasArg().withDescription("set the configuration file")
                .create("cf");
        Option classToCheckOption = OptionBuilder.withArgName("classname").withLongOpt("classToCheck").hasArg().withDescription("set the class to be checked")
                .create('c');
        Option methodToCheckOption = OptionBuilder.withArgName("methodname").withLongOpt("methodToCheck").hasArg()
                .withDescription("set the method to be checked").create('m');
        Option dependenciesOption = OptionBuilder.withArgName("classname").withLongOpt("dependencies").hasArgs()
                .withDescription("additional sources to be parsed").create('d');
        Option relevantClassesOption = OptionBuilder.withArgName("classname").withLongOpt("relevantClasses").hasArgs()
                .withDescription("Set the relevant classes to be used").create("rd");
        Option loopsOptions = OptionBuilder.withArgName("integer").withLongOpt("unroll").hasArg().withDescription("set number of loop unrollings").create('u');
        Option bitOptions = OptionBuilder.withArgName("integer").withLongOpt("width").hasArg().withDescription("set bit width").create('w');
        Option instOptions = OptionBuilder.withArgName("integer").withLongOpt("bound").hasArg().withDescription("set class bound").create('b');
        Option skolemizeOption = OptionBuilder.withLongOpt("skolemize").withDescription("set whether or not skolemize").create("sk");
        Option simulateOption = OptionBuilder.withLongOpt("simulate").withDescription("run method instead of checking").create("r");
        Option modularReasoningOption = OptionBuilder.withLongOpt("modularReasoning").withDescription("check method using modular reasoning").create("mr");
        Option relevancyAnalysisOption = OptionBuilder.withLongOpt("relevancyAnalysis").withDescription("calculate the needed relevantClasses").create("ra");
        Option scopeRestrictionOption = OptionBuilder.withLongOpt("scopeRestriction").withDescription("restrict signature scope to value set in -b option")
                .create("sr");
        /*
         * Option noVerifyOption = OptionBuilder.withLongOpt(
         * "noVerify").withDescription(
         * "builds output but does not invoke verification engine").create(
         * "nv");
         */
        Options options = new Options();
        options.addOption(helpOption);
        options.addOption(versionOption);
        options.addOption(configFileOption);
        options.addOption(classToCheckOption);
        options.addOption(methodToCheckOption);
        options.addOption(dependenciesOption);
        options.addOption(relevantClassesOption);
        options.addOption(loopsOptions);
        options.addOption(bitOptions);
        options.addOption(instOptions);
        options.addOption(skolemizeOption);
        options.addOption(simulateOption);
        options.addOption(modularReasoningOption);
        options.addOption(relevancyAnalysisOption);
        options.addOption(scopeRestrictionOption);
        // options.addOption(noVerifyOption)

        String configFileArgument = null;
        Properties overridingProperties = new Properties();
        TacoCustomScope tacoScope = new TacoCustomScope();

        // create the parser
        CommandLineParser parser = new PosixParser();

        try {
            // parse the command line arguments
            CommandLine line = parser.parse(options, args);

            // help
            if (line.hasOption(helpOption.getOpt())) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp(120, CMD, HEADER, options, FOOTER, true);
                return;
            }

            // version
            if (line.hasOption(versionOption.getOpt())) {
                System.out.println(FOOTER);
                System.out.println("");
                return;
            }

            // Configuration file
            if (line.hasOption(configFileOption.getOpt())) {
                configFileArgument = line.getOptionValue(configFileOption.getOpt());
            }

            // class to check
            if (line.hasOption(classToCheckOption.getOpt())) {
                overridingProperties.put(TacoConfigurator.CLASS_TO_CHECK_FIELD, line.getOptionValue(classToCheckOption.getOpt()));
            }

            // method to check
            if (line.hasOption(methodToCheckOption.getOpt())) {
                String methodtoCheck = line.getOptionValue(methodToCheckOption.getOpt());

                if (!methodtoCheck.matches("^[A-Za-z0-9_-]+_[0-9]")) {
                    methodtoCheck = methodtoCheck + "_0";
                }
                overridingProperties.put(TacoConfigurator.METHOD_TO_CHECK_FIELD, methodtoCheck);
            }

            // Dependencies classes
            if (line.hasOption(dependenciesOption.getOpt())) {
                String dependenciesClasses = "";
                for (String aDependencyClass : line.getOptionValues(dependenciesOption.getOpt())) {
                    dependenciesClasses += aDependencyClass;
                }
                overridingProperties.put(TacoConfigurator.CLASSES_FIELD, dependenciesClasses);
            }

            // Relevant classes
            if (line.hasOption(relevantClassesOption.getOpt())) {
                String relevantClasses = "";
                for (String aRelevantClass : line.getOptionValues(relevantClassesOption.getOpt())) {
                    relevantClasses += aRelevantClass;
                }
                overridingProperties.put(TacoConfigurator.RELEVANT_CLASSES, relevantClasses);
            }

            // Loop unrolling
            if (line.hasOption(loopsOptions.getOpt())) {
                loopUnrolling = Integer.parseInt(line.getOptionValue(loopsOptions.getOpt()));
            }

            // Int bitwidth
            if (line.hasOption(bitOptions.getOpt())) {
                String alloy_bitwidth_str = line.getOptionValue(bitOptions.getOpt());
                overridingProperties.put(TacoConfigurator.BITWIDTH, alloy_bitwidth_str);
                int alloy_bitwidth = new Integer(alloy_bitwidth_str);
                tacoScope.setAlloyBitwidth(alloy_bitwidth);
            }

            // instances scope
            if (line.hasOption(instOptions.getOpt())) {
                String assertionsArguments = "for " + line.getOptionValue(instOptions.getOpt());
                overridingProperties.put(TacoConfigurator.ASSERTION_ARGUMENTS, assertionsArguments);
            }

            // Skolemize
            if (line.hasOption(skolemizeOption.getOpt())) {
                overridingProperties.put(TacoConfigurator.SKOLEMIZE_INSTANCE_INVARIANT, false);
                overridingProperties.put(TacoConfigurator.SKOLEMIZE_INSTANCE_ABSTRACTION, false);
            }

            // Simulation
            if (line.hasOption(simulateOption.getOpt())) {
                overridingProperties.put(TacoConfigurator.INCLUDE_SIMULATION_PROGRAM_DECLARATION, true);
                overridingProperties.put(TacoConfigurator.GENERATE_CHECK, false);
                overridingProperties.put(TacoConfigurator.GENERATE_RUN, false);
            }

            // Modular Reasoning
            if (line.hasOption(modularReasoningOption.getOpt())) {
                overridingProperties.put(TacoConfigurator.MODULAR_REASONING, true);
            }

            // Relevancy Analysis
            if (line.hasOption(relevancyAnalysisOption.getOpt())) {
                overridingProperties.put(TacoConfigurator.RELEVANCY_ANALYSIS, true);
            }

        } catch (ParseException e) {
            System.err.println("Command line parsing failed: " + e.getMessage());
        }

        try {
            System.out.println("****** Starting Taco (version. " + tacoVersion + ") ****** ");
            System.out.println("");



            TacoMain main = new TacoMain(null);

            // BUILD TacoScope

            main.run(configFileArgument, overridingProperties);

        } catch (IllegalArgumentException e) {
            System.err.println("Error found:");
            System.err.println(e.getMessage());
        } catch (MethodToCheckNotFoundException e) {
            System.err.println("Error found:");
            System.err.println("Method to check was not found. Please verify config file, or add -m option");
        } catch (TacoException e) {
            System.err.println("Error found:");
            System.err.println(e.getMessage());
        }
    }

    public TacoMain(HashMap<String, Object> inputToFix){
        this.inputToFix = inputToFix;
    }


    public void run(String configFile) throws IllegalArgumentException {
        this.run(configFile, new Properties());
    }


    public void runDriver(String configFile, Properties overridingProperties) throws IllegalArgumentException {
        if (configFile == null) {
            throw new IllegalArgumentException("Config file not found, please verify option -cf");
        }

        List<JCompilationUnitType> compilation_units = null;
        String classToCheck = null;
        String methodToCheck = overridingProperties.getProperty(TacoConfigurator.METHOD_TO_CHECK_FIELD);

        // Start configurator
        JDynAlloyConfig.reset();
        JDynAlloyConfig.buildConfig(configFile, overridingProperties);

        List<JDynAlloyModule> jdynalloy_modules = new ArrayList<JDynAlloyModule>();
        SimpleJmlToJDynAlloyContext simpleJmlToJDynAlloyContext;
        if (TacoConfigurator.getInstance().getBoolean(TacoConfigurator.JMLPARSER_ENABLED, TacoConfigurator.JMLPARSER_ENABLED_DEFAULT)) {
            // JAVA PARSING
            String sourceRootDir = TacoConfigurator.getInstance().getString(TacoConfigurator.JMLPARSER_SOURCE_PATH_STR);

            if (TacoConfigurator.getInstance().getString(TacoConfigurator.CLASS_TO_CHECK_FIELD) == null) {
                throw new TacoException("Config key 'CLASS_TO_CHECK_FIELD' is mandatory. Please check your config file or add the -c parameter");
            }
            List<String> files = new ArrayList<String>(Arrays.asList(JDynAlloyConfig.getInstance().getClasses()));
            classToCheck = TacoConfigurator.getInstance().getString(TacoConfigurator.CLASS_TO_CHECK_FIELD);
            if (!files.contains(classToCheck)) {
                files.add(classToCheck);
            }


            String userDir = System.getProperty("user.dir") + System.getProperty("file.separator") + "bin";
            boolean compilationSuccess = JmlParser.getInstance().initialize(sourceRootDir, userDir /* Unused */, files);

            compilation_units = JmlParser.getInstance().getCompilationUnits();
            // END JAVA PARSING

            // BEGIN SIMPLIFICATION
            JmlStage aJavaCodeSimplifier = new JmlStage(compilation_units);
            aJavaCodeSimplifier.execute();
            JmlToSimpleJmlContext jmlToSimpleJmlContext = aJavaCodeSimplifier.getJmlToSimpleJmlContext();
            List<JCompilationUnitType> simplified_compilation_units = aJavaCodeSimplifier.get_simplified_compilation_units();

            // END SIMPLIFICATION

            // BEGIN JAVA TO JDYNALLOY TRANSLATION
            // JDynAlloy modules have Alloy contracts and dynAlloy programs
            SimpleJmlStage aJavaToJDynAlloyTranslator = new SimpleJmlStage(simplified_compilation_units);
            //HERE IS WHERE THE PREDS AND VARS ARE PRODUCED
            aJavaToJDynAlloyTranslator.execute();
            // END JAVA TO JDYNALLOY TRANSLATION

            simpleJmlToJDynAlloyContext = aJavaToJDynAlloyTranslator.getSimpleJmlToJDynAlloyContext();

            // JFSL TO JDYNALLOY TRANSLATION
            JfslStage aJfslToDynJAlloyTranslator = new JfslStage(simplified_compilation_units, aJavaToJDynAlloyTranslator.getModules(), jmlToSimpleJmlContext,
                    simpleJmlToJDynAlloyContext);
            aJfslToDynJAlloyTranslator.execute();

            aJfslToDynJAlloyTranslator = null;
            // END JFSL TO JDYNALLOY TRANSLATION

            // PRINT JDYNALLOY
            JDynAlloyPrinterStage printerStage = new JDynAlloyPrinterStage(aJavaToJDynAlloyTranslator.getModules());
            printerStage.execute();
            printerStage = null;
            // END PRINT JDYNALLOY

            jdynalloy_modules.addAll(aJavaToJDynAlloyTranslator.getModules());

        } else {
            simpleJmlToJDynAlloyContext = null;
        }

        // JDYNALLOY BUILT-IN MODULES
        PrecompiledModules precompiledModules = null;
        if (this.inputToFix != null){
            precompiledModules = new PrecompiledModules((HashMap<String, Object>)inputToFix);
        } else {
            precompiledModules = new PrecompiledModules();
        }
        precompiledModules.execute();
        jdynalloy_modules.addAll(precompiledModules.getModules());
        // END JDYNALLOY BUILT-IN MODULES

        // JDYNALLOY STATIC FIELDS CLASS
        JDynAlloyModule staticFieldsModule = precompiledModules.generateStaticFieldsModule();
        jdynalloy_modules.add(staticFieldsModule);
        /**/	staticFieldsModule = null;
        // END JDYNALLOY STATIC FIELDS CLASS

        // JDYNALLOY PARSING
        if (TacoConfigurator.getInstance().getBoolean(TacoConfigurator.JDYNALLOY_PARSER_ENABLED, TacoConfigurator.JDYNALLOY_PARSER_ENABLED_DEFAULT)) {
            JDynAlloyParsingStage jDynAlloyParser = new JDynAlloyParsingStage(jdynalloy_modules);
            jDynAlloyParser.execute();
            jdynalloy_modules.addAll(jDynAlloyParser.getParsedModules());
            /**/		jDynAlloyParser = null;
        }
        // END JDYNALLOY PARSING

        // BEGIN JDYNALLOY TO DYNALLOY TRANSLATION
        String methodToCheckWithoutTyping = overridingProperties.getProperty("methodToCheck").substring(0, overridingProperties.getProperty("methodToCheck").indexOf('('));
        JDynAlloyStage dynJAlloyToDynAlloyTranslator = new JDynAlloyStage(jdynalloy_modules, overridingProperties.getProperty("classToCheck"), methodToCheckWithoutTyping, inputToFix);
        dynJAlloyToDynAlloyTranslator.setJavaArithmetic(TacoConfigurator.getInstance().getUseJavaArithmetic());
        dynJAlloyToDynAlloyTranslator.setRemoveQuantifiers(TacoConfigurator.getInstance().getRemoveQuantifiers());
        dynJAlloyToDynAlloyTranslator.execute();
        // END JDYNALLOY TO DYNALLOY TRANSLATION

        AlloyAnalysisResult alloy_analysis_result = null;
        DynalloyStage dynalloyToAlloy = null;

        // GRAB PREDICATES COMING FROM ARITHMETIC EXPRESSIONS
        HashMap<String, AlloyTyping> varsAndTheirTypesComingFromArithmeticConstraintsInContractsByProgram = new HashMap<String, AlloyTyping>();
        HashMap<String, List<AlloyFormula>> predsComingFromArithmeticConstraintsInContractsByProgram = new HashMap<String, List<AlloyFormula>>();

        HashMap<String, AlloyTyping> varsAndTheirTypesComingFromArithmeticConstraintsInObjectInvariantsByModule = new HashMap<String,AlloyTyping>();
        HashMap<String, List<AlloyFormula>> predsComingFromArithmeticConstraintsInObjectInvariantsByModule = new HashMap<String, List<AlloyFormula>> ();

        for (DynalloyModule dm : dynJAlloyToDynAlloyTranslator.getGeneratedModules()){
            String modName = dm.getModuleId();
            varsAndTheirTypesComingFromArithmeticConstraintsInObjectInvariantsByModule.put(modName, dm.getVarsComingFromArithmeticConstraintsInObjectInvariants());
            predsComingFromArithmeticConstraintsInObjectInvariantsByModule.put(modName, dm.getPredsComingFromArithmeticConstraintsInObjectInvariants());
            Set<ProgramDeclaration> progs = dm.getPrograms();
            for (ProgramDeclaration pd : progs){
                varsAndTheirTypesComingFromArithmeticConstraintsInContractsByProgram.put(pd.getProgramId(), pd.getVarsFromArithInContracts());
                predsComingFromArithmeticConstraintsInContractsByProgram.put(pd.getProgramId(), pd.getPredsFromArithInContracts());
            }
        }

        // DYNALLOY TO ALLOY TRANSLATION
        if (TacoConfigurator.getInstance().getBoolean(TacoConfigurator.DYNALLOY_TO_ALLOY_ENABLE)) {

            dynalloyToAlloy = new DynalloyStage(dynJAlloyToDynAlloyTranslator.getOutputFileNames(),
                    varsAndTheirTypesComingFromArithmeticConstraintsInObjectInvariantsByModule,
                    predsComingFromArithmeticConstraintsInObjectInvariantsByModule,
                    varsAndTheirTypesComingFromArithmeticConstraintsInContractsByProgram,
                    predsComingFromArithmeticConstraintsInContractsByProgram, inputToFix);

            dynalloyToAlloy.setSourceJDynAlloy(dynJAlloyToDynAlloyTranslator.getPrunedModules());
            dynalloyToAlloy.execute();
            // DYNALLOY TO ALLOY TRANSLATION

        }

    }

    /**
     *
     * @param configFile
     * @param overridingProperties
     *            Properties that overrides properties file's values
     */

    @SuppressWarnings("unchecked")
    public void run(String configFile, Properties overridingProperties) throws IllegalArgumentException {

        runDriver(configFile, overridingProperties);
        Properties invProperties = overridingProperties;
//        String oldMethod = invProperties.getProperty(TacoConfigurator.METHOD_TO_CHECK_FIELD);
        invProperties.setProperty(TacoConfigurator.METHOD_TO_CHECK_FIELD, "generateInvariant()");
        runDriver(configFile, invProperties);
        String theGeneratedInvariantAlsFile = TacoConfigurator.getGeneratedInvariantFilename();
        String theActualALS = "";
        try {
            File alsInvFile = new File(TacoConfigurator.getGeneratedInvariantFilename());
            Scanner myReader = new Scanner(alsInvFile);
            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                theActualALS += "\r\n" + data;
            }
            myReader.close();
            alsInvFile.delete();

            theActualALS = theActualALS.replace("fact {\r\n", "fact {");
            String classToCheck = TacoConfigurator.getInstance().getClassToCheck();
            String fullyQualifiedMethodToCheck = classToCheck + "_" + "generateInvariant";
            String preconditionPred = "fact {  precondition" + "_" + fullyQualifiedMethodToCheck + "[(QF";
            boolean test = theActualALS.contains(preconditionPred);
            theActualALS = theActualALS.replace(preconditionPred, "run {  " + classToCheck + "_object_invariant" + "[(QF");
            preconditionPred = "fact {  precondition" + "_" + fullyQualifiedMethodToCheck + "[QF";
            theActualALS = theActualALS.replace(preconditionPred, "run {  " + classToCheck + "_object_invariant" + "[QF");

            int indexRun = theActualALS.indexOf("run {");
            String startingInRun = theActualALS.substring(indexRun);
            int indexClosingBracket = startingInRun.indexOf(']');
            String startingInRunUntilClosingBracket = startingInRun.substring(0,indexClosingBracket+1);
            
            String runWithoutThrows = removeThrows(startingInRunUntilClosingBracket);
            theActualALS = theActualALS.replace(startingInRunUntilClosingBracket, runWithoutThrows);

            int indexAllScopes = theActualALS.indexOf("for 0 but");
            String scopesHoldingSuffix = theActualALS.substring(indexAllScopes);
            int posEndScopes = scopesHoldingSuffix.indexOf('\r');
            String allScopes = scopesHoldingSuffix.substring(0, posEndScopes);

            indexRun = theActualALS.indexOf("run {");
            startingInRun =  theActualALS.substring(indexRun);
            int indexClosingBrace = startingInRun.indexOf('}');
            String runUntilClosingBrace = theActualALS.substring(indexRun, indexRun + indexClosingBrace + 1);
            String runWithScopes = runUntilClosingBrace + " " + allScopes;
            theActualALS = theActualALS.replace(runUntilClosingBrace, runWithScopes);

            int posCheck = theActualALS.indexOf("check check");
            String fromChechCheck = theActualALS.substring(posCheck);
            int posCRLN = fromChechCheck.indexOf("\r\n");
            String theCheckCheckUntilEOL = fromChechCheck.substring(0, posCRLN + 2);
            theActualALS = theActualALS.replace(theCheckCheckUntilEOL, "");
            String oldFileName = TacoConfigurator.getGeneratedInvariantFilename();
            String newFileName = TacoConfigurator.getGeneratedInvariantFilename().replace("als", "inv");
            PrintWriter theNewFile = new PrintWriter(newFileName);
            theNewFile.append(theActualALS);
            theNewFile.checkError();
            theNewFile.close();




        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }


        
    }

    private String removeThrows(String theString) {
        theString = theString.replace("\r\n", "");
        boolean hasTwoBlanks = theString.contains("  ");
        while (hasTwoBlanks){
            theString = theString.replace("  ", "");
            hasTwoBlanks = theString.contains("  ");
        }
        boolean hasThrow = theString.contains("QF.throw");
        while (hasThrow){
            int posLastThrow = theString.lastIndexOf("QF.throw_");
            String pendingString = theString.substring(posLastThrow);
            if (pendingString.contains(",")){
                int posFirstComma = pendingString.indexOf(',');
                String actualThrowIncludingComma = pendingString.substring(0,posFirstComma+1);
                theString = theString.replace(actualThrowIncludingComma, "");
            } else { //there has to be a closing bracket
                int posBracket = pendingString.indexOf(']');
                String actualThrow = pendingString.substring(0,posBracket);
                theString = theString.replace(actualThrow, "");
                if (theString.contains(",]")) {
                    theString = theString.replace(",]", "]");
                }
                if (theString.contains(",,")) {
                    theString = theString.replace(",,", ",");
                }
            }
            hasThrow = theString.contains("QF.throw");
        }

        return theString;
    }

    /**
     *
     */
    private static String getManifestAttribute(Name name) {
        String manifestAttributeValue = "Undefined";
        try {

            String jarFileName = System.getProperty("java.class.path").split(System.getProperty("path.separator"))[0];
            JarFile jar = new JarFile(jarFileName);
            Manifest manifest = jar.getManifest();

            Attributes mainAttributes = manifest.getMainAttributes();
            manifestAttributeValue = mainAttributes.getValue(name);
            jar.close();
        } catch (IOException e) {
        }

        return manifestAttributeValue;
    }

    public static String editTestFileToCompile(String junitFile, String sourceClassName, String classPackage, String methodName) {
        String tmpDir = junitFile.substring(0, junitFile.lastIndexOf(FILE_SEP));
        tmpDir = tmpDir.replaceAll("generated", "output");
        File destFile = new File(tmpDir,obtainClassNameFromFileName(junitFile)+ /*"_temp" +*/ ".java");
        String packageSentence = "package "+classPackage+";\n";
        int posLastUnderscore = methodName.lastIndexOf("_");
        methodName = methodName.substring(0, posLastUnderscore);
        try {
            destFile.createNewFile();
            FileOutputStream fos = new FileOutputStream(destFile);
            boolean packageAlreadyWritten = false;
            Scanner scan = new Scanner(new File(junitFile));
            scan.useDelimiter("\n");
            boolean nextToTest = false;
            String str = null;
            boolean reachedSecondConstructorFromAnalyzedClass = false;
            boolean translatingGetInstance = true;
            while(scan.hasNext()){
                str = scan.next();
                if( nextToTest ) {
                    str = str.replace("()","(String fileClasspath, String className, String methodName) throws IllegalAccessException, InvocationTargetException, ClassNotFoundException, InstantiationException, MalformedURLException");
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                    nextToTest = false;
                    //				} else if (str.contains("public class")){
                    //					int posOpeningBrace = str.indexOf("{");
                    //					str = str.substring(0, posOpeningBrace-1);
                    //					str = str + "_temp {";
                    //					fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                } else if( str.contains("package") && !packageAlreadyWritten){
                    fos.write(packageSentence.getBytes(Charset.forName("UTF-8")));
                    str = "           import java.util.Arrays;";
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                    str = "           import java.net.URL;";
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                    str = "           import java.net.URLClassLoader;";
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                    str = "           import java.net.MalformedURLException;";
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                    str = "           import java.io.File;";
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                    str = "           import java.lang.reflect.InvocationTargetException;";
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                    packageAlreadyWritten = true;
                } else if (str.contains("import") && !packageAlreadyWritten) {
                    fos.write(packageSentence.getBytes(Charset.forName("UTF-8")));
                    fos.write((scan.next() + "\n").getBytes(Charset.forName("UTF-8")));
                    packageAlreadyWritten = true;
                } else if (str.contains("new " + sourceClassName+"(") && !reachedSecondConstructorFromAnalyzedClass
                        && !translatingGetInstance){
                    //		          str = "        try {";
                    //		          fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                    str = "           String[] classpaths = fileClasspath.split(System.getProperty(\"path.separator\"));";
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                    str = "           URL[] urls = new URL[classpaths.length];";
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                    str = "           for (int i = 0 ; i < classpaths.length ; ++i) {";
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                    str = "              urls[i] = new File(classpaths[i]).toURI().toURL();";
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                    str = "           }";
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                    str = "           ClassLoader cl2 = new URLClassLoader(urls);";
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                    //		          str = "           ClassLoaderTools.addFile(fileClasspath);";
                    //		          fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                    str = "           Class<?> clazz = cl2.loadClass(className);";
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                    str = "           Constructor<?>[] c = clazz.getDeclaredConstructors();";
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                    str = "           Object instance = null;";
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                    str = "           	Class<?>[] parameterTypes = null;";
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                    str = "           	Object[] paramValues = null;";
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                    str = "           	Constructor<?> co = null;";
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                    str = "           if (c.length > 0) {";
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                    str = "           	co = c[0];";
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                    str = "           	co.setAccessible(true);";
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                    str = "           	parameterTypes = co.getParameterTypes();";
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                    str = "           	paramValues = new Object[co.getParameterTypes().length];";
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                    str = "             for (int paramIndexer = 0; paramIndexer<parameterTypes.length; paramIndexer++){";
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                    str = "           		if (parameterTypes[paramIndexer].isPrimitive()){";
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                    str = "           			String typeSimpleName = parameterTypes[paramIndexer].getSimpleName();";
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                    str = "           			if (typeSimpleName.equals(\"boolean\")) {";
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                    str = "                         paramValues[paramIndexer] = false;";
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                    str = "                     } else if (typeSimpleName.endsWith(\"byte\")) {";
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                    str = "                       	paramValues[paramIndexer] = 0;";
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                    str = "                     } else if (typeSimpleName.endsWith(\"char\")) {";
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                    str = "                       	paramValues[paramIndexer] = 0;";
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                    str = "                     } else if (typeSimpleName.endsWith(\"double\")) {";
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                    str = "                       	paramValues[paramIndexer] = 0.0d;";
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                    str = "                     } else if (typeSimpleName.endsWith(\"float\")) {";
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                    str = "                       	paramValues[paramIndexer] = 0.0f;";
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                    str = "                     } else if (typeSimpleName.endsWith(\"int\")) {";
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                    str = "                       	paramValues[paramIndexer] = 0;";
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                    str = "                     } else if (typeSimpleName.endsWith(\"long\")) {";
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                    str = "                       	paramValues[paramIndexer] = 0L;";
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                    str = "                     } else if (typeSimpleName.endsWith(\"short\")) {";
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                    str = "                       	paramValues[paramIndexer] = 0;";
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                    str = "                     } else {";
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                    str = "                         System.out.println(\"ERROR: Undefined primitive type.\");";
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                    str = "                     }";
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                    str = "           		} else {";
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                    str = "           			paramValues[paramIndexer] = null;";
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                    str = "           		}";
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                    str = "           	}";
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                    str = "           	try {";
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                    str = "             	String dataCall = co.getName() + Arrays.toString(paramValues);";
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                    str = "             	System.out.println(dataCall);";
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                    str = "           	    instance = co.newInstance(paramValues);";
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                    str = "           	} catch (InstantiationException e) {";
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                    str = "           		e.printStackTrace();";
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                    str = "           	}";
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                    str = "           } else {";
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                    str = "           	System.out.println(\"The class under analysis has no constructors, and at least one should exist.\");";
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                    str = "           }";
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
//                    str = "           Object instance = clazz.newInstance();";
//                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                    reachedSecondConstructorFromAnalyzedClass = true;
                } else if (str.contains("//endGetInstance")) {
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                    translatingGetInstance = false;
                    reachedSecondConstructorFromAnalyzedClass = false;
                } else if (str.contains("Class<?> clazz;")) {
                } else if (str.contains("new " + sourceClassName+"(") && ! translatingGetInstance) {
                    String backup = str;
                    String objectName = backup.split("[ ]+")[2];
                    str = "             Object " + objectName + " = null;";
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                    str = "           	try {";
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                    str = "             	String dataCall = co.getName() + Arrays.toString(paramValues);";
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                    str = "             	System.out.println(dataCall);";
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                    str = "           	   " + objectName + " = co.newInstance(paramValues);";
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                    str = "           	} catch (InstantiationException e) {";
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                    str = "           		e.printStackTrace();";
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                    str = "           	}";
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                } else if (str.contains("} catch (ClassNotFoundException e) {")) {
                    str = str.replace("ClassNotFoundException", "Exception");
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                } else if( str.matches(".*(?i)[\\.a-z0-9\\_]*"+sourceClassName+"(?=[^a-z0-9\\_\\.]).*")){
                    str = str.replaceAll("(?i)[\\.a-z0-9\\_]*"+sourceClassName+"(?=[^a-z0-9\\_\\.])", /*classPackage+"."+*/sourceClassName);
                    str = str.replace("\""+methodName+"\"", "methodName");
                    str = str.replace("\""+sourceClassName+"\"", "clazz");
                    //					str = str.replace("(", "(fileClasspath, ");
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                } else if (str.contains("e.printStackTrace();")) {
                    //					fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                    fos.write(("           throw(new java.lang.RuntimeException(e));" + "\n").getBytes(Charset.forName("UTF-8")));
                    //					fos.write(("throw e;" + "\n").getBytes(Charset.forName("UTF-8")));
                } else if (str.contains("private Method getAccessibleMethod")) {
                    str = str.replace("(String className, ", "(Class<?> clazz, ");
                    //					str = str.replace(") {", ") throws MalformedURLException {");
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                } else if (str.contains("private Constructor<?> getAccessibleConstructor")) {
                    str = str.replace("(String className, ", "(Class<?> clazz, ");
                    //                  str = str.replace(") {", ") throws MalformedURLException {");
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                } else if (str.contains("method.invoke(instance,")) {
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                    str = "           instance = null;";
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                    str = "           method = null;";
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));

                } else if (str.contains("methodToCheck = clazz.getDeclaredMethod(methodName, parameterTypes);")) {
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                } else if (str.contains("clazz = Class.forName(className);")) {
                    //					str = "           ClassLoader cl = ClassLoader.getSystemClassLoader();";
                    //					fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                    //					str = "           final ClassLoader cl2 = new URLClassLoader(new URL[]{new File(fileClasspath).toURI().toURL()}, cl);";
                    //					fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                    //					str = "           clazz = cl2.loadClass(className);";
                    //					fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                    //					str = "           System.out.println(\"actual class inside method: \"+clazz.getName());";
                    //					fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                } else {
                    if (str.contains("@Test")) {
                        nextToTest = true;
                    }
                    //					if (!scan.hasNext()){
                    //						String s = "        } catch (ClassNotFoundException e){";
                    //						fos.write((s + "\n").getBytes(Charset.forName("UTF-8")));
                    //						s = "        } catch (InstantiationException e){}";
                    //						fos.write((s + "\n").getBytes(Charset.forName("UTF-8")));
                    //					}
                    fos.write((str + "\n").getBytes(Charset.forName("UTF-8")));
                }
            }
            fos.close();
            scan.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return destFile.toString();

    }

    private static final int NOT_PRESENT = -1;

    public static String obtainClassNameFromFileName(String fileName) {
        int lastBackslash = fileName.lastIndexOf("/");
        int lastDot = fileName.lastIndexOf(".");

        if (lastBackslash == NOT_PRESENT) {
            lastBackslash = 0;
        } else {
            lastBackslash += 1;
        }
        if (lastDot == NOT_PRESENT) {
            lastDot = fileName.length();
        }

        return fileName.substring(lastBackslash, lastDot);
    }
}


