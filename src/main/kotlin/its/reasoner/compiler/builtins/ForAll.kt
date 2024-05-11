package its.reasoner.compiler.builtins

import org.apache.jena.graph.Node
import org.apache.jena.reasoner.rulesys.BuiltinRegistry
import org.apache.jena.reasoner.rulesys.RuleContext
import org.apache.jena.reasoner.rulesys.builtins.BaseBuiltin

class ForAll : BaseBuiltin() {

    override fun getName(): String = "forAll"

    override fun getArgLength(): Int = 2

    override fun bodyCall(args: Array<out Node>?, length: Int, context: RuleContext?): Boolean {
        checkArgs(length, context)

        val arg0 = getArg(0, args, context)
        val arg1 = getArg(1, args, context)

        val values0 = context?.find(null, arg0, null)?.asSequence()?.map { triple ->
            triple.getObject()
        }
        val values1 = context?.find(null, arg1, null)?.asSequence()?.map { triple ->
            triple.getObject()
        }

        return values0?.all { value0 ->
            values1?.any { value1 ->
                value0.sameValueAs(value1)
            } ?: false
        } ?: false
    }

    companion object {

        init {
            BuiltinRegistry.theRegistry.register(ForAll())
        }
    }
}