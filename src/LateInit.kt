// Cannot be used with primitive types like int, byte,short,long,float,double,char

fun main(){

    val example = LateInit()
    example.initializeName()
    example.printName()


}


class LateInit{
    lateinit var  name: String

    fun initializeName(){
        name = "Kotlin"
    }

    fun printName(){
        if (::name.isInitialized){
            println(name)
        }
        else println(name)
    }
}