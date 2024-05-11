package its.reasoner.compiler.util

object PartialScalePatterns {

    val NUMERATION_RULES_PATTERN = """
            
            [
            (?var1 <partialPredicate> ?var2)
            noValue(?var2, <partialPredicate>, ?var3)
            noValue(?var2, <numberPredicate>)
            makeUniqueID(?var4)
            ->
            (?var2 <numberPredicate> ?var4)
            ]
        
            [
            (?var1 <partialPredicate> ?var2)
            noValue(?var1, <numberPredicate>)
            (?var2 <numberPredicate> ?var3)
            makeUniqueID(?var4)
            strConcat(?var3, "$PARTIAL_SCALE_SEPARATOR", ?var4, ?var5)
            ->
            (?var1 <numberPredicate> ?var5)
            ]
            
        """.trimIndent()

    const val REVERSE_VAR_COUNT = 0
    val REVERSE_PATTERN = """
            (<arg2> <predicate> <arg1>)
        """.trimIndent()

    const val NEGATIVE_REVERSE_VAR_COUNT = 0
    val NEGATIVE_REVERSE_PATTERN = """
            noValue(<arg2>, <predicate>, <arg1>)
        """.trimIndent()

    const val TRANSITIVE_VAR_COUNT = 2
    val TRANSITIVE_PATTERN = """
            (<arg1> <numberPredicate> <var1>)
            (<arg2> <numberPredicate> <var2>)
            startsWith(<var1>, <var2>, "true"^^${XSD_PREF}boolean)
        """.trimIndent()

    const val NEGATIVE_TRANSITIVE_VAR_COUNT = 2
    val NEGATIVE_TRANSITIVE_PATTERN = """
            (<arg1> <numberPredicate> <var1>)
            (<arg2> <numberPredicate> <var2>)
            startsWith(<var1>, <var2>, "false"^^${XSD_PREF}boolean)
        """.trimIndent()

    const val REVERSE_TRANSITIVE_VAR_COUNT = 2
    val REVERSE_TRANSITIVE_PATTERN = """
            (<arg1> <numberPredicate> <var1>)
            (<arg2> <numberPredicate> <var2>)
            startsWith(<var2>, <var1>, "true"^^${XSD_PREF}boolean)
        """.trimIndent()

    const val NEGATIVE_REVERSE_TRANSITIVE_VAR_COUNT = 2
    val NEGATIVE_REVERSE_TRANSITIVE_PATTERN = """
            (<arg1> <numberPredicate> <var1>)
            (<arg2> <numberPredicate> <var2>)
            startsWith(<var2>, <var1>, "false"^^${XSD_PREF}boolean)
        """.trimIndent()

    const val IS_BETWEEN_VAR_COUNT = 3
    val IS_BETWEEN_PATTERN = """
            (<arg1> <numberPredicate> <var1>)
            (<arg2> <numberPredicate> <var2>)
            (<arg3> <numberPredicate> <var3>)
            startsWith(<var1>, <var2>, "true"^^${XSD_PREF}boolean)
            startsWith(<var3>, <var1>, "true"^^${XSD_PREF}boolean)
        """.trimIndent()

    const val IS_CLOSER_TO_THAN_VAR_COUNT = 7
    val IS_CLOSER_TO_THAN_PATTERN = """
            (<arg1> <numberPredicate> <var1>)
            (<arg2> <numberPredicate> <var2>)
            (<arg3> <numberPredicate> <var3>)
            partialScaleDistance(<var2>, <var1>, <var4>)
            partialScaleDistance(<var2>, <var3>, <var5>)
            lessThan(<var4>, <var5>)
        """.trimIndent()

    const val IS_FURTHER_FROM_THAN_VAR_COUNT = 7
    val IS_FURTHER_FROM_THAN_PATTERN = """
            (<arg1> <numberPredicate> <var1>)
            (<arg2> <numberPredicate> <var2>)
            (<arg3> <numberPredicate> <var3>)
            partialScaleDistance(<var2>, <var1>, <var4>)
            partialScaleDistance(<var2>, <var3>, <var5>)
            greaterThan(<var4>, <var5>)
        """.trimIndent()
}