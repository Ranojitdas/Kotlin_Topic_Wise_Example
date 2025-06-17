// Smart casts is a process of casting a variable to a certain type

fun main(){

    typeChecker(5)
    typeChecker("hello")

}

fun typeChecker(any: Any){
    when(any){
        // Smart casted to String
        is String -> {
            println("any is String && length is ${any.length}")
        }
        // Smart casted to Int
        is Int ->{
            println("any is int")
        }
    }
}