package its.reasoner.models

import its.model.DomainModel
import its.model.dictionaries.*

class RClassesDictionary : ClassesDictionaryBase<RClassModel>(RClassModel::class)
class REnumsDictionary : EnumsDictionaryBase<REnumModel>(REnumModel::class)
class RPropertiesDictionary : PropertiesDictionaryBase<RPropertyModel>(RPropertyModel::class)
class RRelationshipsDictionary : RelationshipsDictionaryBase<RRelationshipModel>(RRelationshipModel::class)
class RVarsDictionary : DecisionTreeVarsDictionaryBase<RVarModel>(RVarModel::class)

fun DomainModel._static.usesRDictionaries() : Boolean{
    return classesDictionary is RClassesDictionary &&
            enumsDictionary is REnumsDictionary &&
            propertiesDictionary is RPropertiesDictionary &&
            relationshipsDictionary is RRelationshipsDictionary &&
            decisionTreeVarsDictionary is RVarsDictionary
}