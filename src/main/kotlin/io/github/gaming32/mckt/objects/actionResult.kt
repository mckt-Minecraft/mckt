package io.github.gaming32.mckt.objects

enum class ActionResult {
    SUCCESS, CONSUME, CONSUME_PARTIAL, PASS, FAIL;

    companion object {
        fun success(swingHand: Boolean) = if (swingHand) SUCCESS else CONSUME
    }

    fun isAccepted() = this == SUCCESS || this == CONSUME || this == CONSUME_PARTIAL

    fun shouldSwingHand() = this == SUCCESS

    fun shouldIncrementStats() = this == SUCCESS || this == CONSUME
}

data class ActionResultInfo<T>(val result: ActionResult, val value: T)

fun <T> T.success(swingHand: Boolean = true) =
    ActionResultInfo(ActionResult.success(swingHand), this)

fun <T> T.consume() = ActionResultInfo(ActionResult.CONSUME, this)
fun <T> T.pass() = ActionResultInfo(ActionResult.PASS, this)
fun <T> T.fail() = ActionResultInfo(ActionResult.FAIL, this)
