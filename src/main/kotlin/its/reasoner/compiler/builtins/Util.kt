package its.reasoner.compiler.builtins

import org.apache.jena.graph.Node
import org.apache.jena.reasoner.rulesys.Builtin
import org.apache.jena.reasoner.rulesys.BuiltinException
import org.apache.jena.reasoner.rulesys.BuiltinRegistry
import org.apache.jena.reasoner.rulesys.RuleContext

/**
 * Возвращает соответствующую лексическую форму узла
 */
fun Builtin.lex(node: Node, context: RuleContext?): String {
    return when {
        node.isBlank -> node.blankNodeLabel
        node.isURI -> node.uri
        node.isLiteral -> node.literalLexicalForm
        else -> throw BuiltinException(this, context, "Illegal node type: $node")
    }
}

/**
 * Регистрирует все кастомные builtin
 */
fun registerAllCustomBuiltin() {
    BuiltinRegistry.theRegistry.register(AbsoluteValue())
    BuiltinRegistry.theRegistry.register(Bind())
    BuiltinRegistry.theRegistry.register(CheckCombined())
    BuiltinRegistry.theRegistry.register(Combine())
    BuiltinRegistry.theRegistry.register(CountValues())
    BuiltinRegistry.theRegistry.register(ForAll())
    BuiltinRegistry.theRegistry.register(IsReachable())
    BuiltinRegistry.theRegistry.register(MakeUniqueID())
    BuiltinRegistry.theRegistry.register(PartialScaleDistance())
    BuiltinRegistry.theRegistry.register(StartsWith())
}
