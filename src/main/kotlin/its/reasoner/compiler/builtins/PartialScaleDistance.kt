package its.reasoner.compiler.builtins

import its.reasoner.compiler.util.PARTIAL_SCALE_SEPARATOR
import org.apache.jena.graph.Node
import org.apache.jena.reasoner.rulesys.BuiltinRegistry
import org.apache.jena.reasoner.rulesys.RuleContext
import org.apache.jena.reasoner.rulesys.Util
import org.apache.jena.reasoner.rulesys.builtins.BaseBuiltin
import kotlin.math.abs

class PartialScaleDistance : BaseBuiltin() {

    override fun getName(): String = "partialScaleDistance"

    override fun getArgLength(): Int = 3

    override fun bodyCall(args: Array<out Node>?, length: Int, context: RuleContext?): Boolean {
        checkArgs(length, context)

        val lex0 = lex(getArg(0, args, context), context)
        val lex1 = lex(getArg(1, args, context), context)

        return if (lex0.startsWith(lex1) || lex1.startsWith(lex0)) {
            val len0 = lex0.split(PARTIAL_SCALE_SEPARATOR).size
            val len1 = lex1.split(PARTIAL_SCALE_SEPARATOR).size
            val distance = abs(len0 - len1)

            context?.env?.bind(args?.get(2), Util.makeIntNode(distance)) ?: false
        } else {
            false
        }
    }

    companion object {

        init {
            BuiltinRegistry.theRegistry.register(PartialScaleDistance())
        }
    }
}