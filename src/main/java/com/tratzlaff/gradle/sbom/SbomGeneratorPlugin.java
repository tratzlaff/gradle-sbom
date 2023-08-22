package com.tratzlaff.gradle.sbom;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ResolvedDependency;
import org.spdx.library.InvalidSPDXAnalysisException;
import org.spdx.library.model.Relationship;
import org.spdx.library.model.SpdxDocument;
import org.spdx.library.model.SpdxElement;
import org.spdx.library.model.SpdxPackage;
import org.spdx.library.model.enumerations.RelationshipType;
import org.spdx.storage.IModelStore;
import org.spdx.storage.simple.InMemSpdxStore;

import java.util.ArrayDeque;
import java.util.HashMap;

public class SbomGeneratorPlugin implements Plugin<Project> {
    private final ObjectMapper objectMapper;

    public SbomGeneratorPlugin() {
        this.objectMapper = new ObjectMapper();
        // Configuring ObjectMapper to avoid an error serializing SPDX document to JSON.
        // com.fasterxml.jackson.databind.exc.InvalidDefinitionException:
        // No serializer found for class org.spdx.library.ModelCopyManager and no properties discovered to
        // create BeanSerializer (to avoid exception, disable SerializationFeature.FAIL_ON_EMPTY_BEANS)
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }
    @Override
    public void apply(Project project) {
        project.getTasks().create("generateSpdxSbom", task -> {
            task.doLast(action -> generateSpdx(project));
        });

        project.getTasks().withType(org.gradle.api.tasks.bundling.Jar.class).configureEach(jarTask -> {
            jarTask.dependsOn("generateSpdx");
        });
    }

    private void generateSpdx(Project project) {
        IModelStore modelStore = new InMemSpdxStore();
        String docNamespace = "https://tratzlaff.com/" + project.getGroup() + "/" + project.getName() + "/" + project.getVersion();
        project.getLogger().lifecycle("Generating SPDX document with namespace: " + docNamespace);
        try {
            SpdxDocument spdxDocument = new SpdxDocument(modelStore, docNamespace, null, true);
            spdxDocument.setName(project.getName());

            project
                .getConfigurations()
                .getByName("runtimeClasspath")
                .getResolvedConfiguration()
                .getFirstLevelModuleDependencies()
                .forEach(rootDependency -> {
                    try {
                        processDependency(rootDependency, spdxDocument);
                    } catch (InvalidSPDXAnalysisException e) {
                        throw new RuntimeException(e);
                    }
                });

            try {
                String spdxJson = objectMapper.writeValueAsString(spdxDocument);
                project.getLogger().lifecycle(spdxJson); // debug, info, lifecycle, warning, quiet, error
            } catch (Exception e) {
                project.getLogger().error("Error serializing SPDX document to JSON", e);
            }

        } catch (Exception e) {
            project.getLogger().error("Error generating SPDX document", e);
        }
    }

    private void processDependency(
        ResolvedDependency rootDependency,
        SpdxDocument spdxDocument
    ) throws InvalidSPDXAnalysisException {
        var childToParent = new HashMap<ResolvedDependency, SpdxPackage>();
        var stack = new ArrayDeque<ResolvedDependency>();
        stack.push(rootDependency);

        while(!stack.isEmpty()) {
            var currentDependency = stack.pop();
            var currentParent = childToParent.get(currentDependency);

            var group = currentDependency.getModuleGroup();
            var name = currentDependency.getModuleName();
            var version = currentDependency.getModuleVersion();

            //TODO: Add license information
            //AnyLicenseInfo noAssertionLicense = new SpdxNoAssertionLicense();

            // ID for this object - must be unique within the SPDX document
            var uniqueId = group + ":" + name + ":" + version;

            SpdxPackage spdxPackage = new SpdxPackage(
                spdxDocument.getModelStore(),
                spdxDocument.getDocumentUri(),
                uniqueId,
                spdxDocument.getCopyManager(),
                true
            );
            spdxPackage.setName(name);
            spdxPackage.setVersionInfo(version);
            //spdxPackage.setLicenseDeclared(noAssertionLicense);

            addRelationship(spdxDocument, spdxPackage, RelationshipType.DESCRIBES, null);

            if (currentParent != null) {
                addRelationship(spdxDocument, currentParent, RelationshipType.DEPENDS_ON, null);
            }

            // Process child dependencies
            for (ResolvedDependency childDependency : currentDependency.getChildren()) {
                stack.push(childDependency);
                childToParent.put(childDependency, spdxPackage);
            }
        }
    }

    private void addRelationship(
        SpdxDocument spdxDocument,
        SpdxElement relatedElement,
        RelationshipType relationshipType,
        String comment
    ) throws InvalidSPDXAnalysisException {
        Relationship relationship = new Relationship(
            spdxDocument.getModelStore(),
            spdxDocument.getDocumentUri(),
            spdxDocument.getId()+relatedElement.getId(),
            spdxDocument.getCopyManager(),
            true
        );
        relationship.setRelatedSpdxElement(relatedElement);
        relationship.setRelationshipType(relationshipType);
        if(comment != null && !comment.isEmpty()) relationship.setComment(comment);
        spdxDocument.addRelationship(relationship);
    }
}
