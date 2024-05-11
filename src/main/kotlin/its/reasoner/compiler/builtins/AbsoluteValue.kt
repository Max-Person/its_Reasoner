package its.reasoner.compiler.builtins

import org.apache.jena.graph.Node
import org.apache.jena.reasoner.rulesys.BuiltinRegistry
import org.apache.jena.reasoner.rulesys.RuleContext
import org.apache.jena.reasoner.rulesys.Util
import org.apache.jena.reasoner.rulesys.builtins.BaseBuiltin
import kotlin.math.abs

class AbsoluteValue : BaseBuiltin() {

    override fun getName(): String = "absoluteValue"

    override fun getArgLength(): Int = 2

    override fun bodyCall(args: Array<out Node>?, length: Int, context: RuleContext?): Boolean {
        checkArgs(length, context)
        val arg = getArg(0, args, context)
        return if (Util.isNumeric(arg)) {
            try {
                val value = arg.literalValue.toString().toInt()
                val newValue = Util.makeIntNode(abs(value))
                context?.env?.bind(args?.get(1), newValue) ?: false
            } catch (_: NumberFormatException) {
                try {
                    val value = arg.literalValue.toString().toDouble()
                    val newValue = Util.makeDoubleNode(abs(value))
                    context?.env?.bind(args?.get(1), newValue) ?: false
                } catch (_: NumberFormatException) {
                    false
                }
            }
        } else {
            false
        }
    }

    companion object {

        init {
            BuiltinRegistry.theRegistry.register(AbsoluteValue())
        }
    }
}