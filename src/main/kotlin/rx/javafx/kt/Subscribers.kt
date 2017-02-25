package rx.javafx.kt

import io.reactivex.Flowable
import io.reactivex.FlowableTransformer
import javafx.beans.binding.Binding
import javafx.beans.property.Property
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import rx.javafx.sources.CompositeObservable
import rx.subscriptions.CompositeBinding



/**
 * Binds the `Property` to an RxJava `Observable`,
 * meaning it will be bounded to show the latest emissions of that `Observable`.
 * The `Binding` is also returned so caller can be dispose it later if needed
 * @return `Binding`
 */
fun <T> Property<T>.bind(observable: Observable<T>, actionOp: (ObservableBindingSideEffects<T>.() -> Unit)? = null): Binding<T> {
    val transformer = actionOp?.let {
        val sideEffects = ObservableBindingSideEffects<T>()
        it.invoke(sideEffects)
        sideEffects.transformer
    }
    val binding = (transformer?.let { observable.compose(it) }?:observable).toBinding()
    bind(binding)
    return binding
}

/**
 * Binds the `Property` to an RxJava `Observable`,
 * meaning it will be bounded to show the latest emissions of that `Observable`.
 * The `Binding` is also returned so caller can be dispose it later if needed
 * @return `Binding`
 */
fun <T> Property<T>.bind(flowable: Flowable<T>, actionOp: (FlowableBindingSideEffects<T>.() -> Unit)? = null): Binding<T> {
    val transformer = actionOp?.let {
        val sideEffects = FlowableBindingSideEffects<T>()
        it.invoke(sideEffects)
        sideEffects.transformer
    }
    val binding = (transformer?.let { flowable.compose(it) }?:flowable).toBinding()
    bind(binding)
    return binding
}

/**
 * Add this `Binding` to the provided `CompositeBinding`, and returns itself
 * @return `Binding`
 */
fun <T> Binding<T>.addTo(compositeBinding: CompositeBinding): Binding<T> {
    compositeBinding.add(this)
    return this
}

/**
 * Add this `Observable` to the provided `CompositeObservable`, and return the `Subscription` that links them.
 * You can optionally provide a `CompositeSubscription` to automatically add the `Subscription` to
 */
fun <T> Observable<T>.addTo(compositeObservable: CompositeObservable<T>, compositeSubscription: CompositeDisposable? = null): Disposable {
    val subscription = compositeObservable.add(this)
    compositeSubscription?.apply { this.add(subscription) }
    return subscription
}


operator fun <T> CompositeBinding.plusAssign(binding: Binding<T>) = add(binding)

operator fun CompositeBinding.plusAssign(compositeBinding: CompositeBinding) = add(compositeBinding)

operator fun <T> CompositeBinding.minusAssign(binding: Binding<T>) = remove(binding)

operator fun CompositeBinding.minusAssign(compositeBinding: CompositeBinding) = remove(compositeBinding)

class ObservableBindingSideEffects<T> {
    private var onNextAction: ((T) -> Unit)? = null
    private var onCompleteAction: (() -> Unit)? = null
    private var onErrorAction: ((ex: Throwable) -> Unit)? = null

    fun onNext(onNext: (T) -> Unit): Unit {
        onNextAction = onNext
    }

    fun onCompleted(onComplete: () -> Unit): Unit {
        onCompleteAction = onComplete
    }

    fun onError(onError: (ex: Throwable) -> Unit): Unit {
        onErrorAction = onError
    }

    internal val transformer: ObservableTransformer<T, T> get() = ObservableTransformer<T, T> { obs ->
        var withActions: Observable<T> = obs
        withActions = onNextAction?.let { withActions.doOnNext(onNextAction) } ?: withActions
        withActions = onCompleteAction?.let { withActions.doOnComplete(onCompleteAction) } ?: withActions
        withActions = onErrorAction?.let { withActions.doOnError(onErrorAction) } ?: withActions
        withActions
    }
}


class FlowableBindingSideEffects<T> {
    private var onNextAction: ((T) -> Unit)? = null
    private var onCompleteAction: (() -> Unit)? = null
    private var onErrorAction: ((ex: Throwable) -> Unit)? = null

    fun onNext(onNext: (T) -> Unit): Unit {
        onNextAction = onNext
    }

    fun onCompleted(onComplete: () -> Unit): Unit {
        onCompleteAction = onComplete
    }

    fun onError(onError: (ex: Throwable) -> Unit): Unit {
        onErrorAction = onError
    }

    internal val transformer: FlowableTransformer<T, T> get() = FlowableTransformer<T, T> { obs ->
        var withActions: Flowable<T> = obs
        withActions = onNextAction?.let { withActions.doOnNext(onNextAction) } ?: withActions
        withActions = onCompleteAction?.let { withActions.doOnComplete(onCompleteAction) } ?: withActions
        withActions = onErrorAction?.let { withActions.doOnError(onErrorAction) } ?: withActions
        withActions
    }
}