package its.reasoner

import its.reasoner.util.JenaUtil
import its.reasoner.util.RDFUtil.copy
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.RDFDataMgr

class LearningSituation (
    val model : Model,
    val decisionTreeVariables : MutableMap<String, String> = mutableMapOf(),
) {

    constructor(filename: String) : this(ModelFactory.createDefaultModel().read(RDFDataMgr.open(filename), null)){
        val p = model.getProperty(JenaUtil.genLink(JenaUtil.POAS_PREF, JenaUtil.DECISION_TREE_VAR_PREDICATE))
        decisionTreeVariables.putAll(model.listSubjectsWithProperty(p).toList().map{res -> res.getProperty(p).`object`.asLiteral().string to res.localName}.toMap())
    }

    fun copy(model: Model = this.model.copy(), decisionTreeVariables: MutableMap<String, String> = this.decisionTreeVariables.toMutableMap()) : LearningSituation {
        return LearningSituation(model, decisionTreeVariables)
    }
}