package its.reasoner.compiler

import its.model.TypedVariable
import its.model.definition.Domain
import its.model.definition.types.BooleanType
import its.model.expressions.Operator
import its.model.expressions.literals.*
import its.model.expressions.operators.*
import its.model.expressions.visitors.OperatorBehaviour
import its.reasoner.compiler.util.*

class OperatorJenaCompiler(
    private val domain: Domain,
) : OperatorBehaviour<CompilationResult> {

    private fun Operator.compile(): CompilationResult {
        return use(this@OperatorJenaCompiler)
    }

    fun compileExpression(operator: Operator, addAuxiliaryRules: Boolean = true): CompilationResult {
        with(operator) {
            // Добавляем вспомогательные правила, сгенерированные по словарям
            var rules = if (addAuxiliaryRules) generateAuxiliaryRules() else ""

            // Добавляем паузу
            rules += if (addAuxiliaryRules) PAUSE_MARK else ""

            // Генерируем имена
            val skolemName = genVariableName()
            val resPredicateName = if (operator !is AssignDecisionTreeVar) genPredicateName() else ""

            // Упрощаем выражение
            val expr = semantic()

            // Компилируем оператор
            val result = expr.compile()

            // Добавляем скомпилированные правила в результат
            rules += result.rules

            // Для всех незаконченных правил
            result.bodies.forEach { body ->
                // Если есть незаконченное правило
                if (body.isNotEmpty() && operator !is AssignDecisionTreeVar) {
                    // Генерируем правило и добавляем правило к остальным
                    rules += if (validateAndGet(domain).first == BooleanType && this !is GetPropertyValue) {
                        genBooleanRule(body, skolemName, resPredicateName)
                    } else {
                        genRule(body, skolemName, resPredicateName, result.value)
                    }
                }
            }

            return CompilationResult(value = resPredicateName, rules = rules.replace(DEFINITION_AREA_PLACEHOLDER, ""))
        }
    }

    override fun process(literal: BooleanLiteral): CompilationResult {
        TODO("Not yet implemented")
    }

    override fun process(literal: ClassLiteral): CompilationResult {
        return CompilationResult(value = genURI(POAS_PREF, literal.name))
    }

    override fun process(literal: DecisionTreeVarLiteral): CompilationResult {
        val resVarName = genVariableName()
        return CompilationResult(
            value = resVarName,
            bodies = listOf(
                genTriple(
                    resVarName,
                    VAR_PREDICATE,
                    genValue(literal.name),
                    "Получаем значение переменной ${literal.name}"
                )
            )
        )
    }

    override fun process(literal: DoubleLiteral): CompilationResult {
        TODO("Not yet implemented")
    }

    override fun process(literal: EnumLiteral): CompilationResult {
        return CompilationResult(value = genURI(POAS_PREF, literal.value.valueName))
    }

    override fun process(literal: IntegerLiteral): CompilationResult {
        TODO("Not yet implemented")
    }

    override fun process(literal: ObjectLiteral): CompilationResult {
        TODO("Not yet implemented")
    }

    override fun process(literal: StringLiteral): CompilationResult {
        TODO("Not yet implemented")
    }

    override fun process(literal: VariableLiteral): CompilationResult {
        return CompilationResult(value = genVariable(literal.name))
    }

    override fun process(op: AddRelationshipLink): CompilationResult {
        TODO("Not yet implemented")
    }

    override fun process(op: AssignDecisionTreeVar): CompilationResult {
        // Объявляем переменные
        var completedRules = ""

        // Получаем аргументы
        val arg0 = DecisionTreeVarLiteral(op.variableName)
        val arg1 = op.valueExpr

        // Получаем имя переменной
        val varName = genValue(op.variableName)

        // Компилируем аргументы
        val compiledArg0 = arg0.compile()
        val compiledArg1 = arg1.compile()

        // Передаем завершенные правила дальше
        completedRules += compiledArg0.rules +
                compiledArg1.rules

        // Для всех результатов компиляции
        compiledArg0.bodies.forEach { body0 ->
            compiledArg1.bodies.forEach { body1 ->
                // Собираем правила для аргументов
                val body = body0 + body1

                // Заполняем шаблон
                var rule = DECISION_TREE_VAR_ASSIGN_PATTERN
                rule = rule.replace("<tmp0>", genVariableName())
                rule = rule.replace("<dropped>", genPredicateName())

                rule = rule.replace("<ruleBody>", body)
                rule = rule.replace("<newObjBody>", body1)
                rule = rule.replace("<newObj>", compiledArg1.value)
                rule = rule.replace("<oldObj>", compiledArg0.value)
                rule = rule.replace("<varPredicate>", VAR_PREDICATE)
                rule = rule.replace("<varName>", varName)

                // Добавляем в результат
                completedRules += rule
            }
        }

        return CompilationResult(rules = completedRules)
    }

    override fun process(op: AssignProperty): CompilationResult {
        TODO("Not yet implemented")
    }

    override fun process(op: Block): CompilationResult {
        TODO("Not yet implemented")
    }

    override fun process(op: Cast): CompilationResult {
        TODO("Not yet implemented")
    }

    override fun process(op: CheckClass): CompilationResult {
        // Объявляем переменные
        val bodies = mutableListOf<String>()
        var completedRules = ""

        // Получаем аргументы
        val arg0 = op.objectExpr
        val arg1 = op.classExpr

        // Компилируем аргументы
        val compiledArg0 = arg0.compile()
        val compiledArg1 = arg1.compile()

        // Передаем завершенные правила дальше
        completedRules += compiledArg0.rules + compiledArg1.rules

        // Если класс можно вычислить (вычисляемый класс можно получить только указав его имя т.е. через ClassValue)
        // FIXME: вычисляемых классов пока нет
        if (false) { // arg1 is ClassLiteral && isCalculable((arg1).value)) {
//            // Получаем выражение для вычисления
//            var calculation = ClassesDictionary.getCalcExpr(arg1.value)!!
//            if (isNegative) {
//                calculation = LogicalNot(listOf(calculation))
//            }
//            require(calculation.getResultDataType() == DataType.BooleanDT) {
//                "Выражение для вычисления класса должно иметь тип Boolean."
//            }
//
//            // Компилируем выражение для вычисления
//            val compiledCalculation = calculation.semantic().compile()
//
//            // Передаем завершенные правила дальше
//            completedRules += compiledCalculation.rules
//
//            // Для всех результатов компиляции
//            compiledArg0.bodies.forEach { body0 ->
//                compiledArg1.bodies.forEach { body1 ->
//                    compiledCalculation.bodies.forEach { calculationBody ->
//                        // Собираем правило
//                        val body = body0 + body1 + calculationBody.replace(
//                            "?obj",
//                            compiledArg0.value
//                        )
//
//                        // Добавляем в массив
//                        bodies.add(body)
//                    }
//                }
//            }
        } else {
            // Вспомогательная переменная
            val tmp = genVariableName()

            // Для всех результатов компиляции
            compiledArg0.bodies.forEach { body0 ->
                compiledArg1.bodies.forEach { body1 ->
                    // Собираем правило
                    var body = body0 + body1 // Собираем части первого и второго аргументов

                    // Добавляем проверку класса
                    body += genTriple(
                        compiledArg0.value,
                        CLASS_PREDICATE,
                        tmp
                    ) + genIsReachablePrim(
                        tmp,
                        SUBCLASS_PREDICATE,
                        compiledArg1.value,
                        !op.isNegative()
                    )

                    // Добавляем в массив
                    bodies.add(body)
                }
            }
        }

        return CompilationResult(bodies = bodies, rules = completedRules)
    }

    override fun process(op: CheckPropertyValue): CompilationResult {
        TODO("Not yet implemented")
    }

    override fun process(op: CheckRelationship): CompilationResult {
        // Объявляем переменные
        val bodies = mutableListOf<String>()
        var completedRules = ""

        // Компилируем аргументы
        val compiledArgs = mutableListOf(CompilationResult(value = genURI(POAS_PREF, op.relationshipName)))
        val argValues = mutableListOf(compiledArgs.first().value)

        op.children.forEach { arg ->
            val res = arg.compile()
            compiledArgs.add(res)
            argValues.add(res.value)
            completedRules += res.rules
        }

        // Проверяем кол-во аргументов
        val relName = op.relationshipName

        // Для всех результатов компиляции
        val indices = compiledArgs.map { 0 }.toMutableList()

        val _predicate = genURI(POAS_PREF, relName)
        val _args = op.children
        val _varsCount = if (_args.size == 2) 0 else 1
        val _body = if (_args.size == 2) {
            "(<arg1> $_predicate <arg2>)\n"
        } else {
            var tmp = "(<arg1> ${_predicate}_subj <var1>)\n" // TODO: протестировать _subj

            _args.forEachIndexed { index, _ ->
                if (index != 0) {
                    tmp += "(<var1> ${_predicate}_obj_${index - 1} <arg${index + 1}>)\n"
                }
            }

            tmp
        }

        val _negativeVarsCount = if (_args.size == 2) 0 else null
        val _negativeBody = if (_args.size == 2) {
            "noValue(<arg1>, $_predicate, <arg2>)\n"
        } else {
            null
        }

        // Пока не дошли до первого индекса
        while (indices.first() != compiledArgs.first().bodies.size) {
            // Собираем все части
            var body = ""
            for (i in compiledArgs.indices) {
                body += compiledArgs[i].bodies[indices[i]]
            }

            // Добавляем проверку отношения
            var pattern = if (op.isNegative()) {
                _negativeBody!!
            } else {
                _body
            }

            // Заполняем аргументы
            for (i in _args.indices) {
                pattern = pattern.replace("<arg${i + 1}>", argValues[i + 1])
            }

            // Заполняем переменные
            val varsCount = if (op.isNegative()) {
                _negativeVarsCount!!
            } else {
                _varsCount
            }
            for (i in 0..varsCount) {
                pattern = pattern.replace("<var$i>", genVariableName())
            }

            // Добавляем шаблон
            body += pattern

            // Добавляем в результат
            bodies.add(body)

            // Меняем индексы
            var i = indices.size - 1
            while (true) {
                if (indices[i] != compiledArgs[i].bodies.size - 1 || i == 0) {
                    ++indices[i]
                    break
                } else {
                    indices[i] = 0
                }

                --i
            }
        }

        return CompilationResult(bodies = bodies, rules = completedRules)
    }

    override fun process(op: Compare): CompilationResult {
        TODO("Not yet implemented")
    }

    override fun process(op: CompareWithComparisonOperator): CompilationResult {
        // Объявляем переменные
        val bodies = mutableListOf<String>()
        var completedRules = ""

        // Получаем аргументы
        val arg0 = op.firstExpr
        val arg1 = op.secondExpr

        // Компилируем аргументы
        val compiledArg0 = arg0.compile()
        val compiledArg1 = arg1.compile()

        // Передаем завершенные правила дальше
        completedRules += compiledArg0.rules + compiledArg1.rules

        var operator = op.operator

        // Если нужно отрицание
        if (op.isNegative()) {
            // Меням оператор на противоположный
            operator = when (op.operator) {
                CompareWithComparisonOperator.ComparisonOperator.Less -> CompareWithComparisonOperator.ComparisonOperator.GreaterEqual
                CompareWithComparisonOperator.ComparisonOperator.Greater -> CompareWithComparisonOperator.ComparisonOperator.LessEqual
                CompareWithComparisonOperator.ComparisonOperator.Equal -> CompareWithComparisonOperator.ComparisonOperator.NotEqual
                CompareWithComparisonOperator.ComparisonOperator.LessEqual -> CompareWithComparisonOperator.ComparisonOperator.Greater
                CompareWithComparisonOperator.ComparisonOperator.GreaterEqual -> CompareWithComparisonOperator.ComparisonOperator.Less
                CompareWithComparisonOperator.ComparisonOperator.NotEqual -> CompareWithComparisonOperator.ComparisonOperator.Equal
            }
        }

        // Для всех результатов компиляции
        compiledArg0.bodies.forEach { body0 ->
            compiledArg1.bodies.forEach { body1 ->
                var body = body0 + body1

                // Добавляем проверку соответствующего оператору примитива
                body += when (operator) {
                    CompareWithComparisonOperator.ComparisonOperator.Less -> {
                        genLessThanPrim(compiledArg0.value, compiledArg1.value)
                    }

                    CompareWithComparisonOperator.ComparisonOperator.Greater -> {
                        genGreaterThanPrim(compiledArg0.value, compiledArg1.value)
                    }

                    CompareWithComparisonOperator.ComparisonOperator.Equal -> {
                        genEqualPrim(compiledArg0.value, compiledArg1.value)
                    }

                    CompareWithComparisonOperator.ComparisonOperator.LessEqual -> {
                        genLessEqualPrim(compiledArg0.value, compiledArg1.value)
                    }

                    CompareWithComparisonOperator.ComparisonOperator.GreaterEqual -> {
                        genGreaterEqualPrim(compiledArg0.value, compiledArg1.value)
                    }

                    CompareWithComparisonOperator.ComparisonOperator.NotEqual -> {
                        genNotEqualPrim(compiledArg0.value, compiledArg1.value)
                    }
                }

                // Добавляем в массив
                bodies.add(body)
            }
        }

        return CompilationResult(bodies = bodies, rules = completedRules)
    }

    override fun process(op: ExistenceQuantifier): CompilationResult {
        // Объявляем переменные
        val bodies = mutableListOf<String>()
        var completedRules = ""

        // Получаем аргументы
        val arg0 = op.selectorExpr
        val arg1 = op.conditionExpr

        // Компилируем аргументы
        val compiledArg0 = arg0.compile()
        val compiledArg1 = arg1.compile()

        // Передаем завершенные правила дальше
        completedRules += compiledArg0.rules + compiledArg1.rules

        if (op.isNegative()) {
            // Флаг, указывающий на объекты множества
            val objectsFlag = genPredicateName()
            // Skolem name
            val skolemName = genVariableName()

//            if (arg1 is HasNegativeForm && !arg1.isNegative()) { // TODO нормальное условие или что это вообще тут надо???
            if (false) { // TODO нормальное условие или что это вообще тут надо???
                // Для всех результатов компиляции
                compiledArg0.bodies.forEach { body0 ->
                    compiledArg1.bodies.forEach { body1 ->
                        val body = body0 + body1

                        val contextVars = op.mContext.map {
                            genVariable(it)
                        } + genVariable(op.variable.varName)

                        val rule = if (op.mContext.isEmpty()) {
                            genBooleanRule(body, skolemName, objectsFlag)
                        } else {
                            genBooleanRule(body, skolemName, objectsFlag, genVariableName(), contextVars)
                        }
//                    val rule = genBooleanRule(body, skolemName, objectsFlag, genVariableName(), contextVars)
                        completedRules += rule






                        completedRules += PAUSE_MARK

                        val tmp = genVariableName()

//                    val contextVars = mContext.map {
//                        genVariable(it)
//                    } + genVariable(varName)

                        var tmp2 = ""
                        contextVars.forEach {
                            tmp2 += it + ", "
                        }
                        tmp2 = tmp2.dropLast(2)

                        val checkBody = if (op.mContext.isEmpty()) {
                            body0 + body1 + genNoValuePrim(skolemName, objectsFlag)
                        } else {
                            body0 + body1 + genTriple(
                                skolemName,
                                objectsFlag,
                                tmp
                            ) + "checkCombined(\"false\"^^http://www.w3.org/2001/XMLSchema#boolean, $skolemName, $objectsFlag, $tmp2)"
                        }
//                val checkBody = body0 + genNoValuePrim(skolemName, objectsFlag)
//                val checkBody = body0 + genTriple(skolemName, objectsFlag, tmp) + "checkCombined(\"false\"^^http://www.w3.org/2001/XMLSchema#boolean, $skolemName, $objectsFlag, $tmp2)"
                        // Добавляем в массив
                        bodies.add(checkBody)
                    }


                }
            } else {
                // Для всех результатов компиляции
                compiledArg0.bodies.forEach { body0 ->
                    compiledArg1.bodies.forEach { body1 ->
                        val body = body0 + body1

                        val contextVars = op.mContext.map {
                            genVariable(it)
                        } + genVariable(op.variable.varName)

                        val rule = if (op.mContext.isEmpty()) {
                            genBooleanRule(body, skolemName, objectsFlag)
                        } else {
                            genBooleanRule(body, skolemName, objectsFlag, genVariableName(), contextVars)
                        }
//                    val rule = genBooleanRule(body, skolemName, objectsFlag, genVariableName(), contextVars)
                        completedRules += rule
                    }

                    completedRules += PAUSE_MARK

                    val tmp = genVariableName()

                    val contextVars = op.mContext.map {
                        genVariable(it)
                    } //+ genVariable(varName)

                    var tmp2 = ""
                    contextVars.forEach {
                        tmp2 += it + ", "
                    }
                    tmp2 = tmp2.dropLast(2)

                    val checkBody = if (op.mContext.isEmpty()) {
                        body0 + genNoValuePrim(skolemName, objectsFlag)
                    } else {
                        body0 + genTriple(
                            skolemName,
                            objectsFlag,
                            tmp
                        ) + "checkCombined(\"false\"^^http://www.w3.org/2001/XMLSchema#boolean, $skolemName, $objectsFlag, $tmp2)"
                    }
//                val checkBody = body0 + genNoValuePrim(skolemName, objectsFlag)
//                val checkBody = body0 + genTriple(skolemName, objectsFlag, tmp) + "checkCombined(\"false\"^^http://www.w3.org/2001/XMLSchema#boolean, $skolemName, $objectsFlag, $tmp2)"
                    // Добавляем в массив
                    bodies.add(checkBody)
                }
            }
        } else {
            // Для всех результатов компиляции
            compiledArg0.bodies.forEach { body0 ->
                compiledArg1.bodies.forEach { body1 ->
                    val body = body0 + body1

                    // Добавляем в массив
                    bodies.add(body)
                }
            }
        }

        return CompilationResult(bodies = bodies, rules = completedRules)
    }

    override fun process(op: ForAllQuantifier): CompilationResult {
        TODO("Not yet implemented")
    }

    override fun process(op: GetByCondition): CompilationResult {
        // Получаем аргументы
        val arg0 = op.conditionExpr

        // Компилируем аргументы
        val compiledArg0 = arg0.compile()

        return CompilationResult(value = genVariable(op.variable.varName), bodies = compiledArg0.bodies, rules = compiledArg0.rules)
    }

    override fun process(op: GetByRelationship): CompilationResult {
        // Получаем аргументы
        val arg0 = op.subjectExpr
        val arg1 = op.relationshipName

        // Генерируем имя вспомогательной переменной
        val varName = genVariableName()

        // Компилируем через GetByCondition
        return GetByCondition(
            variable = TypedVariable(className = "Item", varName = varName),
            conditionExpr = CheckRelationship(arg0, arg1, listOf(VariableLiteral(varName)))
        ).semantic().compile()
    }

    override fun process(op: GetClass): CompilationResult {
        TODO("Not yet implemented")
    }

    override fun process(op: GetExtreme): CompilationResult {
        TODO("Not yet implemented")
    }

    override fun process(op: GetPropertyValue): CompilationResult {
        // Объявляем переменные
        val value = genVariableName()
        val bodies = mutableListOf<String>()
        var completedRules = ""

        // Получаем аргументы
        val arg0 = op.objectExpr
        val arg1 = op.propertyName

        // Компилируем аргументы
        val compiledArg0 = arg0.compile()
        val compiledArg1 = CompilationResult(value = genURI(POAS_PREF, arg1))

        // Передаем завершенные правила дальше
        completedRules += compiledArg0.rules +
                compiledArg1.rules

        // Имя свойства
        val propName = op.propertyName

        // Если свойство не статическое FIXME: пока нет статических свойств
        if (true) { // !PropertiesDictionary.isStatic(propName)) {
            // Для всех результатов компиляции
            compiledArg0.bodies.forEach { body0 ->
                compiledArg1.bodies.forEach { body1 ->
                    var body = body0 + body1

                    // Добавляем получение свойства
                    body += genTriple(
                        compiledArg0.value,
                        compiledArg1.value,
                        value,
                        "Получаем свойство объекта"
                    )

                    // Добавляем в массив
                    bodies.add(body)
                }
            }
        } else {
            // Вспомогательные переменные
            val empty0 = genVariableName()
            val empty1 = genVariableName()
            val empty2 = genVariableName()

            // Флаг, указывающий на классы объекта с заданным свойством
            val classWithPropFlag = genPredicateName()
            // Переменная с классом
            val classVar = genVariableName()
            // Skolem name
            val skolemName = genVariableName()

            // Флаг цикла
            val cycleFlag = genPredicateName()
            // Переменная цикла
            val cycleVar = genVariableName()
            // Флаг, указывающий на классы, которые уже были проверены и не подошли по критериям
            val dropped = genPredicateName()

            // Переменные аргументов
            val ruleArg1 = genVariableName()
            val ruleArg2 = genVariableName()
            val ruleArg3 = genVariableName()

            // Для всех результатов компиляции
            compiledArg0.bodies.forEach { body0 ->
                compiledArg1.bodies.forEach { body1 ->

                    // ---------------- Генерируем правило, помечающее классы объекта --------------

                    var body = DEFINITION_AREA_PLACEHOLDER + body0 + body1

                    // Получаем класс
                    body += genTriple(
                        compiledArg0.value,
                        CLASS_PREDICATE,
                        empty0,
                        "Получаем класс объекта"
                    ) + genIsReachablePrim(
                        empty0,
                        SUBCLASS_PREDICATE,
                        classVar,
                        true
                    )

                    // Добавляем проверку наличия свойства
                    body += genTriple(
                        classVar,
                        compiledArg1.value,
                        empty1,
                        "Проверяем, что у класса есть свойство с любым значением"
                    )

                    // Добавляем в результат
                    completedRules += genRule(body, skolemName, classWithPropFlag, classVar)
                }
            }

            // ---------------- Генерируем правило, помечающее потенциальный экстремум --------------

            // Собираем правило, организующее цикл
            val cycleBody = genNoValuePrim(
                subj = empty0,
                predicate = cycleFlag,
                comment = "Проверяем отсутствие флага цикла"
            ) + genTriple(
                subj = empty1,
                predicate = classWithPropFlag,
                obj = cycleVar,
                comment = "Записываем в переменную цикла класс со свойством"
            ) + genNoValuePrim(
                subj = empty2,
                predicate = dropped,
                obj = cycleVar,
                comment = "Проверяем, что класс еще не был проверен"
            )

            // Добавляем в результат
            completedRules += genRule(cycleBody, skolemName, cycleFlag, cycleVar)

            // ---------------- Генерируем правило, проверяющее экстремум --------------

            // Собираем правило, выбирающее ближайший класс

            // Инициализируем аргумент 1 - элемент цикла
            var filterBody = genTriple(
                empty0,
                cycleFlag,
                ruleArg1,
                "Записываем в переменную 1 текущий объект цикла"
            )
            // Инициализируем аргумент 2 - самый удаленный от объекта класс (типа Object в Java)
            filterBody += genNoValuePrim(
                subj = ruleArg2,
                predicate = SUBCLASS_PREDICATE,
                comment = "Записываем в переменную 2 класс, не имеющий родителя (самый удаленный)"
            )
            // Инициализируем аргумент 3 - класс со свойством
            filterBody += genTriple(
                empty1,
                classWithPropFlag,
                ruleArg3,
                "Записываем в переменную 3 класс со свойством"
            )

            // Вычисляем самый удаленный класс

            // Получаем шаблон
            var relPattern = PartialScalePatterns.IS_CLOSER_TO_THAN_PATTERN
            relPattern =
                relPattern.replace("<numberPredicate>", SUBCLASS_NUMERATION_PREDICATE)

            // Заполняем аргументы
            relPattern = relPattern.replace("<arg1>", ruleArg1)
            relPattern = relPattern.replace("<arg2>", ruleArg2)
            relPattern = relPattern.replace("<arg3>", ruleArg3)

            // Заполняем временные переменные
            val varCount = PartialScalePatterns.IS_CLOSER_TO_THAN_VAR_COUNT
            for (i in 1..varCount) {
                relPattern = relPattern.replace("<var$i>", genVariableName())
            }

            // Добавляем заполненный шаблон
            filterBody += relPattern

            // Генерируем правило
            var filterRule = EXTREME_CLASS_PATTER
            filterRule = filterRule.replace("<ruleBody>", filterBody)
            filterRule = filterRule.replace("<skolemName>", skolemName)
            filterRule = filterRule.replace("<obj>", ruleArg1)
            filterRule = filterRule.replace("<dropped>", dropped)

            // Добавляем в основное правило
            val mainBody = genTriple(
                empty0,
                cycleFlag,
                classVar,
                "Получаем класс, который остался после цикла"
            ) + genTriple(
                classVar,
                compiledArg1.value,
                value,
                "Получаем значение свойства у этого класса"
            )

            // Добавляем в результат
            bodies.add(mainBody)
            completedRules += filterRule

            // Добавляем паузу
            completedRules += PAUSE_MARK
        }

        return CompilationResult(value = value, bodies = bodies, rules = completedRules)
    }

    override fun process(op: IfThen): CompilationResult {
        TODO("Not yet implemented")
    }

    override fun process(op: LogicalAnd): CompilationResult {
        // Объявляем переменные
        val bodies = mutableListOf<String>()
        var completedRules = ""

        // Получаем аргументы
        val arg0 = op.firstExpr
        val arg1 = op.secondExpr

        // Компилируем аргументы
        val compiledArg0 = arg0.compile()
        val compiledArg1 = arg1.compile()

        // Передаем завершенные правила дальше
        completedRules += compiledArg0.rules +
                compiledArg1.rules

        // Для всех результатов компиляции
        compiledArg0.bodies.forEach { body0 ->
            compiledArg1.bodies.forEach { body1 ->
                val body = body0 + body1

                // Добавляем в массив
                bodies.add(body)
            }
        }

        return CompilationResult(bodies = bodies, rules = completedRules)
    }

    override fun process(op: LogicalNot): CompilationResult {
        TODO("Not yet implemented")
    }

    override fun process(op: LogicalOr): CompilationResult {
        TODO("Not yet implemented")
    }

    override fun process(op: With): CompilationResult {
        TODO("Not yet implemented")
    }

    companion object {

        /**
         * Шаблон правила присваивания значения переменной дерева мысли
         */
        private val DECISION_TREE_VAR_ASSIGN_PATTERN = """
            [
            <ruleBody>
            ->
            drop(0)
            (<oldObj> <dropped> "true"^^${XSD_PREF}boolean)
            ]
            
            ${PAUSE_MARK.trim()}
            
            [
            (<oldObj> <dropped> <tmp0>)
            <newObjBody>
            ->
            (<newObj> <varPredicate> <varName>)
            ]
            
            [
            noValue(<tmp0>, <dropped>)
            <newObjBody>
            ->
            (<newObj> <varPredicate> <varName>)
            ]
        """.trimIndent()

        /**
         * Шаблон правила выбора экстремального класса
         */
        private val EXTREME_CLASS_PATTER = """
            
            [
            <ruleBody>makeSkolem(<skolemName>)
            ->
            drop(0)
            (<skolemName> <dropped> <obj>)
            ]
            
        """.trimIndent()
    }
}