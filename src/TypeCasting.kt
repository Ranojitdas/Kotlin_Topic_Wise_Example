
fun main(){

    val obj: Any = "Hello"

    val str: String = obj as String // not safe casting,throw exception for null
    val str1: String? = obj as? String // safe casting

    val intval = "56".toInt()
    val floatval = "45.85".toFloat()
}