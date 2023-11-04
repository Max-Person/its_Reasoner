package its.reasoner.util

import its.model.DomainModel
import its.model.expressions.types.Clazz
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.Property
import org.apache.jena.rdf.model.Resource

object RDFUtil {
    @JvmStatic
    fun Resource.asObj() : RDFObj {
        return RDFObj(this)
    }

    @JvmStatic
    fun Resource.asClazz() : Clazz {
        require(DomainModel.classesDictionary.exist(localName)){"Attempting to map RDF Resource $localName onto a class, but this class is not present in the classes dictionary"}
        return DomainModel.classesDictionary.get(localName)!!
    }

    @JvmStatic
    fun Clazz.asResource(model: Model) : Resource {
        return model.resource(this.name)
    }

    @JvmStatic
    fun Resource.getLineage(property: Property, outgoing : Boolean) : List<Resource>{
        var current : Resource? = this

        val lineage = mutableListOf<Resource>()
        while(current != null){
            lineage.add(current)
            if(outgoing)
                current = current.getProperty(property)?.`object`?.asResource()
            else
                current = model.listSubjectsWithProperty(property, current).toList().singleOrNull()
        }

        return lineage
    }

    @JvmStatic
    fun Resource.getLineageExclusive(property: Property, outgoing : Boolean) : List<Resource>{
        return this.getLineage(property, outgoing).minus(this)
    }

    @JvmStatic
    fun Model.resource(name : String) : Resource {
        return this.getResource(JenaUtil.genLink(JenaUtil.POAS_PREF, name))
    }

    @JvmStatic
    fun Model.getObjects() : List<RDFObj>{
        val CLASS_PREDICATE_NAME = "type"

        val property = this.getProperty(JenaUtil.genLink(JenaUtil.RDF_PREF, CLASS_PREDICATE_NAME))
        return this.listSubjectsWithProperty(property).toList().map{ RDFObj(it) }
    }

    @JvmStatic
    fun Model.copy() : Model{
        return ModelFactory.createDefaultModel().add(this)
    }
}