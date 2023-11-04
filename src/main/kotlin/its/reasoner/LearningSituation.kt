package its.reasoner

import its.model.DomainModel
import its.model.expressions.types.Obj
import its.reasoner.util.JenaUtil
import its.reasoner.util.RDFUtil.asObj
import its.reasoner.util.RDFUtil.copy
import its.reasoner.util.RDFUtil.resource
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.RDFDataMgr

open class LearningSituation (
    val model : Model,
    val decisionTreeVariables : MutableMap<String, String>,
) {

    companion object _static{
        @JvmStatic
        fun collectDecisionTreeVariables(model: Model): MutableMap<String, String>{
            val p = model.getProperty(JenaUtil.genLink(JenaUtil.POAS_PREF, JenaUtil.DECISION_TREE_VAR_PREDICATE))
            return  model.listSubjectsWithProperty(p).toList()
                .associate { res -> res.getProperty(p).`object`.asLiteral().string to res.localName }.toMutableMap()
        }
    }

    constructor(model : Model) : this(model, collectDecisionTreeVariables(model))

    constructor(filename: String) : this(ModelFactory.createDefaultModel().read(RDFDataMgr.open(filename), null, "TTL").add(DomainModel.domainRDF))

    fun copy(model: Model = this.model.copy(), decisionTreeVariables: MutableMap<String, String> = this.decisionTreeVariables.toMutableMap()) : LearningSituation {
        return LearningSituation(model, decisionTreeVariables)
    }

    fun variableValue(varName: String) : Obj{
        return model.resource(decisionTreeVariables[varName]!!).asObj()
    }

    fun objByName(alias: String) : Obj{
        return model.resource(alias).asObj()
    }
}