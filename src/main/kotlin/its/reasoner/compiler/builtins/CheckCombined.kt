package its.reasoner.compiler.builtins

import org.apache.jena.graph.Node
import org.apache.jena.reasoner.rulesys.BuiltinRegistry
import org.apache.jena.reasoner.rulesys.RuleContext
import org.apache.jena.reasoner.rulesys.builtins.BaseBuiltin

class CheckCombined : BaseBuiltin() {

    override fun getName(): String = "checkCombined"

    override fun bodyCall(args: Array<out Node>?, length: Int, context: RuleContext?): Boolean {
        checkArgs(length, context)

        val expected = getArg(0, args, context).literalValue.toString().toBoolean()
        val obj = getArg(1, args, context)
        val rel = getArg(2, args, context)

        val vars = args!!.drop(3)

        var res = false

        context?.find(obj, rel, null)?.asSequence()?.forEach { triple ->
            val combined = triple.getObject().literalValue.toString().split("§")

//            res = ((vars.size == combined.size) && vars.all {
//                combined.contains(lex(it, context))
//            }) || res

            res = vars.all {
                // TODO: надо еще протестировать, подойдет ли это поведения во всех случаях
                val index = vars.indexOf(it)
                combined[index] == lex(it, context)
            } || res
        }

        return res == expected
    }

    companion object {

        init {
            BuiltinRegistry.theRegistry.register(CheckCombined())
        }
    }
}