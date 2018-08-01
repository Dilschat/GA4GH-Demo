package picard.util.help;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.RootDoc;

import org.broadinstitute.barclay.argparser.CommandLineProgramProperties;
import org.broadinstitute.barclay.help.DocumentedFeature;
import org.broadinstitute.barclay.help.DocWorkUnit;
import org.broadinstitute.barclay.help.GSONWorkUnit;
import org.broadinstitute.barclay.help.HelpDoclet;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Custom Barclay-based Javadoc Doclet used for generating Picard help/documentation.
 *
 * NOTE: Methods in this class are intended to be called by Gradle/Javadoc only, and should not be called
 * by methods that are used by the Picard runtime. This class has a dependency on com.sun.javadoc classes,
 * which may not be present since they're not provided as part of the normal Picard runtime classpath.
 */
public class PicardHelpDoclet extends HelpDoclet {

    private final static String PICARD_FREEMARKER_INDEX_TEMPLATE_NAME = "generic.index.template.html";

    /**
     * Create a doclet of the appropriate type and generate the FreeMarker templates properties.
     * @param rootDoc
     * @throws IOException
     */
    public static boolean start(final RootDoc rootDoc) throws IOException {
        return new picard.util.help.PicardHelpDoclet().startProcessDocs(rootDoc);
    }

    /**
     * Return the name of the freemarker template to be used for the index generated by Barclay.
     * Must reside in the folder passed to the Barclay Javadc Doclet via the "-settings-dir" parameter.
     * @return name of freemarker index template
     */
    @Override
    public String getIndexTemplateName() {
        return PICARD_FREEMARKER_INDEX_TEMPLATE_NAME;
    }

    /**
     * @return Create and return a DocWorkUnit-derived object to handle documentation
     * for the target feature(s) represented by documentedFeature.
     *
     * @param documentedFeature DocumentedFeature annotation for the target feature
     * @param classDoc javadoc classDoc for the target feature
     * @param clazz class of the target feature
     * @return DocWorkUnit to be used for this feature
     */
    @Override
    protected DocWorkUnit createWorkUnit(
            final DocumentedFeature documentedFeature,
            final ClassDoc classDoc,
            final Class<?> clazz)
    {
        return new DocWorkUnit(
                new PicardHelpDocWorkUnitHandler(this),
                documentedFeature,
                classDoc,
                clazz);
    }

    /**
     * Adds a super-category so that we can custom-order the categories in the doc index
     *
     * @param docWorkUnit
     * @return root Map after having added the super-category
     */
    @Override
    protected final Map<String, String> getGroupMap(final DocWorkUnit docWorkUnit) {
        final Map<String, String> root = super.getGroupMap(docWorkUnit);

        /**
         * Add-on super-category definitions. The super-category and spark value strings need to be the
         * same as used in the Freemarker template.
         */
        root.put("supercat", HelpConstants.getSuperCategoryProperty(docWorkUnit.getGroupName()));

        return root;
    }

}
