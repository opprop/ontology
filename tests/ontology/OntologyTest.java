package ontology;

import checkers.inference.test.CFInferenceTest;

import ontology.solvers.backend.OntologySolverEngine;

import org.checkerframework.framework.test.TestUtilities;
import org.checkerframework.javacutil.Pair;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class OntologyTest extends CFInferenceTest {

    public OntologyTest(File testFile) {
        super(
                testFile,
                ontology.OntologyChecker.class,
                "ontology",
                "-Anomsgtext",
                "-d",
                "tests/build/outputdir");
    }

    @Override
    public boolean useHacks() {
        return true;
    }

    @Override
    public Pair<String, List<String>> getSolverNameAndOptions() {
        final String solverName = OntologySolverEngine.class.getCanonicalName();
        List<String> solverOptions = new ArrayList<>();
        solverOptions.add("solver=Z3");
        return Pair.<String, List<String>>of(solverName, solverOptions);
    }

    @Parameters
    public static List<File> getTestFiles() {
        List<File> testfiles = new ArrayList<>();
        testfiles.addAll(TestUtilities.getJavaFilesAsArgumentList(new File("testing")));
        return testfiles;
    }
}
