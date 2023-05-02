package its.reasoner.util

import its.model.expressions.types.Clazz
import its.model.expressions.types.Obj
import its.reasoner.util.RDFUtil.asClazz
import its.reasoner.util.RDFUtil.asResource
import its.reasoner.util.RDFUtil.getLineage
import org.apache.jena.rdf.model.Resource

data class RDFObj(
    val resource: Resource,
) : Obj {
    override val name: String = resource.localName
    private val model get() = resource.model

    override val clazz: Clazz
        get() {
            val CLASS_PREDICATE_NAME = "type"
            val classProp = resource.model.getProperty(JenaUtil.genLink(JenaUtil.RDF_PREF, CLASS_PREDICATE_NAME))
            val classRes = this.resource.getProperty(classProp).`object`.asResource()

            return classRes.asClazz()

        }

    override val classInheritance: List<Clazz>
        get() {
            val classRes = this.clazz.asResource(model)

            val SUBCLASS_PREDICATE_NAME = "subClassOf"
            val subclassProp = model.getProperty(JenaUtil.genLink(JenaUtil.RDFS_PREF, SUBCLASS_PREDICATE_NAME))

            return classRes.getLineage(subclassProp, true).map{it.asClazz()}

        }

    override fun descriptionInfo(descriptionProperty: String): Any {
        val rdfProperty = model.getProperty(JenaUtil.genLink(JenaUtil.POAS_PREF, descriptionProperty))
        return resource.getProperty(rdfProperty)?.`object`!!.asLiteral().value
    }

}