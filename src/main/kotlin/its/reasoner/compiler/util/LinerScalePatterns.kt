package its.reasoner.compiler.util

object LinerScalePatterns {

    val NUMERATION_RULES_PATTERN = """
            
            [
            (?var1 <linerPredicate> ?var2)
            noValue(?var3, <linerPredicate>, ?var1)
            noValue(?var1, <numberPredicate>)
            ->
            (?var1 <numberPredicate> "1"^^${XSD_PREF}integer)
            ]
        
            [
            (?var1 <linerPredicate> ?var2)
            noValue(?var2, <numberPredicate>)
            (?var1 <numberPredicate> ?var3)
            addOne(?var3, ?var4)
            ->
            (?var2 <numberPredicate> ?var4)
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
            lessThan(<var1>, <var2>)
        """.trimIndent()

    const val NEGATIVE_TRANSITIVE_VAR_COUNT = 2
    val NEGATIVE_TRANSITIVE_PATTERN = """
            (<arg1> <numberPredicate> <var1>)
            (<arg2> <numberPredicate> <var2>)
            ge(<var1>, <var2>)
        """.trimIndent()

    const val REVERSE_TRANSITIVE_VAR_COUNT = 2
    val REVERSE_TRANSITIVE_PATTERN = """
            (<arg1> <numberPredicate> <var1>)
            (<arg2> <numberPredicate> <var2>)
            greaterThan(<var1>, <var2>)
        """.trimIndent()

    const val NEGATIVE_REVERSE_TRANSITIVE_VAR_COUNT = 2
    val NEGATIVE_REVERSE_TRANSITIVE_PATTERN = """
            (<arg1> <numberPredicate> <var1>)
            (<arg2> <numberPredicate> <var2>)
            le(<var1>, <var2>)
        """.trimIndent()

    const val IS_BETWEEN_VAR_COUNT = 3
    val IS_BETWEEN_PATTERN = """
            (<arg1> <numberPredicate> <var1>)
            (<arg2> <numberPredicate> <var2>)
            (<arg3> <numberPredicate> <var3>)
            greaterThan(<var1>, <var2>)
            lessThan(<var1>, <var3>)
        """.trimIndent()

    const val IS_CLOSER_TO_THAN_VAR_COUNT = 7
    val IS_CLOSER_TO_THAN_PATTERN = """
            (<arg1> <numberPredicate> <var1>)
            (<arg2> <numberPredicate> <var2>)
            (<arg3> <numberPredicate> <var3>)
            difference(<var2>, <var1>, <var4>)
            difference(<var2>, <var3>, <var5>)
            absoluteValue(<var4>, <var6>)
            absoluteValue(<var5>, <var7>)
            lessThan(<var6>, <var7>)
        """.trimIndent()

    const val IS_FURTHER_FROM_THAN_VAR_COUNT = 7
    val IS_FURTHER_FROM_THAN_PATTERN = """
            (<arg1> <numberPredicate> <var1>)
            (<arg2> <numberPredicate> <var2>)
            (<arg3> <numberPredicate> <var3>)
            difference(<var2>, <var1>, <var4>)
            difference(<var2>, <var3>, <var5>)
            absoluteValue(<var4>, <var6>)
            absoluteValue(<var5>, <var7>)
            greaterThan(<var6>, <var7>)
        """.trimIndent()
}