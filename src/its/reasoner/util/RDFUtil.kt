package its.reasoner.util

import its.model.DomainModel
import its.model.expressions.types.Clazz
import its.model.expressions.types.Obj
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.Resource

object RDFUtil {
    @JvmStatic
    fun Resource.asObj() : Obj {
        return Obj(this.localName, this)
    }

    @JvmStatic
    fun Resource.asClazz() : Clazz {
        return DomainModel.classesDictionary.get(localName)!!
    }


    @JvmStatic
    fun Model.resource(name : String) : Resource {
        return this.getResource(JenaUtil.genLink(JenaUtil.POAS_PREF, name))
    }

    @JvmStatic
    fun Model.getObjects() : List<Obj>{
        val CLASS_PREDICATE_NAME = "type"

        val property = this.getProperty(JenaUtil.genLink(JenaUtil.RDF_PREF, CLASS_PREDICATE_NAME))
        return this.listSubjectsWithProperty(property).toList().map{ Obj(it.localName, it) }
    }
}