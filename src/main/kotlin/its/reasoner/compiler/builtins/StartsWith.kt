package its.reasoner.compiler.builtins

import org.apache.jena.graph.Node
import org.apache.jena.reasoner.rulesys.BuiltinRegistry
import org.apache.jena.reasoner.rulesys.RuleContext
import org.apache.jena.reasoner.rulesys.builtins.BaseBuiltin

class StartsWith : BaseBuiltin() {

    override fun getName(): String = "startsWith"

    override fun getArgLength(): Int = 3

    override fun bodyCall(args: Array<out Node>?, length: Int, context: RuleContext?): Boolean {
        checkArgs(length, context)

        val lex0 = lex(getArg(0, args, context), context)
        val lex1 = lex(getArg(1, args, context), context)
        val expected = getArg(2, args, context).literalValue.toString().toBoolean()

        return expected == lex0.startsWith(lex1)
    }

    companion object {

        init {
            BuiltinRegistry.theRegistry.register(StartsWith())
        }
    }
}