package its.reasoner.models

import its.model.models.RelationshipModel
import its.reasoner.util.JenaUtil
import its.reasoner.util.NamingManager

class JenaRelationshipModel(
    name: String,
    parent: String? = null,
    argsClasses: List<String>,
    scaleType: ScaleType? = null,
    scaleRelationshipsNames: List<String>? = listOf(),
    relationType: RelationType? = null,
    flags: Int,
) : RelationshipModel(name, parent, argsClasses, scaleType, scaleRelationshipsNames, relationType, flags) {

    var varsCount: Int
        private set
    var body: String
        private set
    var rules: String? = null
        private set
    var negativeVarsCount: Int
        private set
    var negativeBody: String
        private set
    var negativeRules: String? = null
        private set


    init{
        varsCount = if (argsClasses.size == 2) {
            0
        } else {
            1
        }

        body = if (argsClasses.size == 2) {
            "(<arg1> ${JenaUtil.genLink(JenaUtil.POAS_PREF, name)} <arg2>)\n"
        } else {
            var tmp = "(<arg1> ${JenaUtil.genLink(JenaUtil.POAS_PREF, name)} <var1>)\n"

            argsClasses.forEachIndexed { index, _ ->
                if (index != 0) {
                    tmp += "(<var1> ${JenaUtil.genLink(JenaUtil.POAS_PREF, name)} <arg${index + 1}>)\n"
                }
            }

            tmp
        }

        negativeVarsCount = varsCount

        negativeBody = if (argsClasses.size == 2) {
            "(<arg2> ${JenaUtil.genLink(JenaUtil.POAS_PREF, name)} <arg1>)\n"
        } else {
            "" // TODO
        }
    }

    private constructor(
        name: String,
        parent: String? = null,
        argsClasses: List<String>,
        scaleType: ScaleType? = null,
        scaleRelationshipsNames: List<String>? = null,
        relationType: RelationType? = null,
        flags: Int,
        varsCount: Int,
        body: String,
        rules: String? = null,
        negativeVarsCount: Int,
        negativeBody: String,
        negativeRules: String? = null
    ) : this(name, parent, argsClasses, scaleType, scaleRelationshipsNames, relationType, flags){
        this.varsCount = varsCount
        this.body = body
        this.rules = rules
        this.negativeVarsCount = negativeVarsCount
        this.negativeBody = negativeBody
        this.negativeRules = negativeRules
    }

    private object LinerScalePatterns {

        val NUMERATION_RULES_PATTERN = """
            
            [
            (?var1 <linerPredicate> ?var2)
            noValue(?var3, <linerPredicate>, ?var1)
            noValue(?var1, <numberPredicate>)
            ->
            (?var1 <numberPredicate> "1"^^xsd:integer)
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

        const val TRANSITIVE_CLOSURE_VAR_COUNT = 2
        val TRANSITIVE_CLOSURE_PATTERN = """
            (<arg1> <numberPredicate> <var1>)
            (<arg2> <numberPredicate> <var2>)
            lessThan(<var1>, <var2>)
        """.trimIndent()

        const val REVERSE_TRANSITIVE_CLOSURE_VAR_COUNT = 2
        val REVERSE_TRANSITIVE_CLOSURE_PATTERN = """
            (<arg1> <numberPredicate> <var1>)
            (<arg2> <numberPredicate> <var2>)
            greaterThan(<var1>, <var2>)
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

    private object PartialScalePatterns {

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
            strConcat(?var3, ".", ?var4, ?var5)
            ->
            (?var1 <numberPredicate> ?var5)
            ]
            
        """.trimIndent()

        const val REVERSE_VAR_COUNT = 0
        val REVERSE_PATTERN = """
            (<arg2> <predicate> <arg1>)
        """.trimIndent()

        const val TRANSITIVE_CLOSURE_VAR_COUNT = 2
        val TRANSITIVE_CLOSURE_PATTERN = """
            (<arg1> <numberPredicate> <var1>)
            (<arg2> <numberPredicate> <var2>)
            startsWith(<var2>, <var1>)
        """.trimIndent()

        const val REVERSE_TRANSITIVE_CLOSURE_VAR_COUNT = 2
        val REVERSE_TRANSITIVE_CLOSURE_PATTERN = """
            (<arg1> <numberPredicate> <var1>)
            (<arg2> <numberPredicate> <var2>)
            startsWith(<var1>, <var2>)
        """.trimIndent()

        const val IS_BETWEEN_VAR_COUNT = 3
        val IS_BETWEEN_PATTERN = """
            (<arg1> <numberPredicate> <var1>)
            (<arg2> <numberPredicate> <var2>)
            (<arg3> <numberPredicate> <var3>)
            startsWith(<var1>, <var2>)
            startsWith(<var3>, <var1>)
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


    override fun scaleRelationships(): List<RelationshipModel> {
        val scalePredicate = NamingManager.genPredicateName()
        return when (scaleType) {
            ScaleType.Linear -> listOf(
                JenaRelationshipModel(
                    name = scaleRelationshipsNames!![0],
                    argsClasses = argsClasses,
                    flags = flags,
                    varsCount = LinerScalePatterns.REVERSE_VAR_COUNT,
                    body = LinerScalePatterns.REVERSE_PATTERN.replace(
                        "<predicate>",
                        JenaUtil.genLink(JenaUtil.POAS_PREF, name)
                    ),
                    negativeVarsCount = LinerScalePatterns.REVERSE_VAR_COUNT,
                    negativeBody = "(<arg1> ${JenaUtil.genLink(JenaUtil.POAS_PREF, name)} <arg2>)\n",
                ),
                JenaRelationshipModel(
                    name = scaleRelationshipsNames!![1],
                    argsClasses = argsClasses,
                    flags = 16,
                    varsCount = LinerScalePatterns.TRANSITIVE_CLOSURE_VAR_COUNT,
                    body = LinerScalePatterns.TRANSITIVE_CLOSURE_PATTERN.replace(
                        "<numberPredicate>",
                        JenaUtil.genLink(JenaUtil.POAS_PREF, scalePredicate)
                    ),
                    negativeVarsCount = LinerScalePatterns.TRANSITIVE_CLOSURE_VAR_COUNT,
                    negativeBody = """
            (<arg1> <numberPredicate> <var1>)
            (<arg2> <numberPredicate> <var2>)
            ge(<var1>, <var2>)
        """.trimIndent().replace(
                        "<numberPredicate>",
                        JenaUtil.genLink(JenaUtil.POAS_PREF, scalePredicate)
                    ),
                ),
                JenaRelationshipModel(
                    name = scaleRelationshipsNames!![2],
                    argsClasses = argsClasses,
                    flags = 16,
                    varsCount = LinerScalePatterns.REVERSE_TRANSITIVE_CLOSURE_VAR_COUNT,
                    body = LinerScalePatterns.REVERSE_TRANSITIVE_CLOSURE_PATTERN.replace(
                        "<numberPredicate>",
                        JenaUtil.genLink(JenaUtil.POAS_PREF, scalePredicate)
                    ),
                    negativeVarsCount = LinerScalePatterns.REVERSE_TRANSITIVE_CLOSURE_VAR_COUNT,
                    negativeBody = """
            (<arg1> <numberPredicate> <var1>)
            (<arg2> <numberPredicate> <var2>)
            le(<var1>, <var2>)
        """.trimIndent().replace(
                        "<numberPredicate>",
                        JenaUtil.genLink(JenaUtil.POAS_PREF, scalePredicate)
                    ),
                ),
                JenaRelationshipModel(
                    name = scaleRelationshipsNames!![3],
                    argsClasses = argsClasses.plus(argsClasses[0]),
                    flags = 0,
                    varsCount = LinerScalePatterns.IS_BETWEEN_VAR_COUNT,
                    body = LinerScalePatterns.IS_BETWEEN_PATTERN.replace(
                        "<numberPredicate>",
                        JenaUtil.genLink(JenaUtil.POAS_PREF, scalePredicate)
                    ),
                    negativeVarsCount = LinerScalePatterns.IS_BETWEEN_VAR_COUNT,
                    negativeBody = "", // TODO
                ),
                JenaRelationshipModel(
                    name = scaleRelationshipsNames!![4],
                    argsClasses = argsClasses.plus(argsClasses[0]),
                    flags = 0,
                    varsCount = LinerScalePatterns.IS_CLOSER_TO_THAN_VAR_COUNT,
                    body = LinerScalePatterns.IS_CLOSER_TO_THAN_PATTERN.replace(
                        "<numberPredicate>",
                        JenaUtil.genLink(JenaUtil.POAS_PREF, scalePredicate)
                    ),
                    negativeVarsCount = LinerScalePatterns.IS_CLOSER_TO_THAN_VAR_COUNT,
                    negativeBody = "", // TODO
                ),
                JenaRelationshipModel(
                    name = scaleRelationshipsNames!![5],
                    argsClasses = argsClasses.plus(argsClasses[0]),
                    flags = 0,
                    varsCount = LinerScalePatterns.IS_FURTHER_FROM_THAN_VAR_COUNT,
                    body = LinerScalePatterns.IS_FURTHER_FROM_THAN_PATTERN.replace(
                        "<numberPredicate>",
                        JenaUtil.genLink(JenaUtil.POAS_PREF, scalePredicate)
                    ),
                    negativeVarsCount = LinerScalePatterns.IS_FURTHER_FROM_THAN_VAR_COUNT,
                    negativeBody = "", // TODO
                )
            )

            ScaleType.Partial -> listOf(
                JenaRelationshipModel(
                    name = scaleRelationshipsNames!![0],
                    argsClasses = argsClasses,
                    flags = flags,
                    varsCount = PartialScalePatterns.REVERSE_VAR_COUNT,
                    body = PartialScalePatterns.REVERSE_PATTERN.replace(
                        "<predicate>",
                        JenaUtil.genLink(JenaUtil.POAS_PREF, name)
                    ),
                    negativeVarsCount = PartialScalePatterns.REVERSE_VAR_COUNT,
                    negativeBody = "(<arg1> ${JenaUtil.genLink(JenaUtil.POAS_PREF, name)} <arg2>)\n",
                ),
                JenaRelationshipModel(
                    name = scaleRelationshipsNames!![1],
                    argsClasses = argsClasses,
                    flags = 16,
                    varsCount = PartialScalePatterns.TRANSITIVE_CLOSURE_VAR_COUNT,
                    body = PartialScalePatterns.TRANSITIVE_CLOSURE_PATTERN.replace(
                        "<numberPredicate>",
                        JenaUtil.genLink(JenaUtil.POAS_PREF, scalePredicate)
                    ),
                    negativeVarsCount = PartialScalePatterns.TRANSITIVE_CLOSURE_VAR_COUNT,
                    negativeBody = PartialScalePatterns.REVERSE_TRANSITIVE_CLOSURE_PATTERN.replace(
                        "<numberPredicate>",
                        JenaUtil.genLink(JenaUtil.POAS_PREF, scalePredicate)
                    ),
                ),
                JenaRelationshipModel(
                    name = scaleRelationshipsNames!![2],
                    argsClasses = argsClasses,
                    flags = 16,
                    varsCount = PartialScalePatterns.REVERSE_TRANSITIVE_CLOSURE_VAR_COUNT,
                    body = PartialScalePatterns.REVERSE_TRANSITIVE_CLOSURE_PATTERN.replace(
                        "<numberPredicate>",
                        JenaUtil.genLink(JenaUtil.POAS_PREF, scalePredicate)
                    ),
                    negativeVarsCount = PartialScalePatterns.REVERSE_TRANSITIVE_CLOSURE_VAR_COUNT,
                    negativeBody = PartialScalePatterns.TRANSITIVE_CLOSURE_PATTERN.replace(
                        "<numberPredicate>",
                        JenaUtil.genLink(JenaUtil.POAS_PREF, scalePredicate)
                    ),
                ),
                JenaRelationshipModel(
                    name = scaleRelationshipsNames!![3],
                    argsClasses = argsClasses.plus(argsClasses[0]),
                    flags = 0,
                    varsCount = PartialScalePatterns.IS_BETWEEN_VAR_COUNT,
                    body = PartialScalePatterns.IS_BETWEEN_PATTERN.replace(
                        "<numberPredicate>",
                        JenaUtil.genLink(JenaUtil.POAS_PREF, scalePredicate)
                    ),
                    negativeVarsCount = PartialScalePatterns.IS_BETWEEN_VAR_COUNT,
                    negativeBody = "", // TODO
                ),
                JenaRelationshipModel(
                    name = scaleRelationshipsNames!![4],
                    argsClasses = argsClasses.plus(argsClasses[0]),
                    flags = 0,
                    varsCount = PartialScalePatterns.IS_CLOSER_TO_THAN_VAR_COUNT,
                    body = PartialScalePatterns.IS_CLOSER_TO_THAN_PATTERN.replace(
                        "<numberPredicate>",
                        JenaUtil.genLink(JenaUtil.POAS_PREF, scalePredicate)
                    ),
                    negativeVarsCount = PartialScalePatterns.IS_CLOSER_TO_THAN_VAR_COUNT,
                    negativeBody = "", // TODO
                ),
                JenaRelationshipModel(
                    name = scaleRelationshipsNames!![5],
                    argsClasses = argsClasses.plus(argsClasses[0]),
                    flags = 0,
                    varsCount = PartialScalePatterns.IS_FURTHER_FROM_THAN_VAR_COUNT,
                    body = PartialScalePatterns.IS_FURTHER_FROM_THAN_PATTERN.replace(
                        "<numberPredicate>",
                        JenaUtil.genLink(JenaUtil.POAS_PREF, scalePredicate)
                    ),
                    negativeVarsCount = PartialScalePatterns.IS_FURTHER_FROM_THAN_VAR_COUNT,
                    negativeBody = "", // TODO
                )
            )

            else -> {
                require(flags < 64) {
                    "Некорректный набор флагов."
                }
                return emptyList()
            }
        }
    }
}