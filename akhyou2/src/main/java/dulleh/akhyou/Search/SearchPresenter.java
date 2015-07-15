package dulleh.akhyou.Search;

import android.os.Bundle;

import de.greenrobot.event.EventBus;
import dulleh.akhyou.Models.Anime;
import dulleh.akhyou.Search.Providers.AnimeRushSearchProvider;
import dulleh.akhyou.Search.Providers.SearchProvider;
import dulleh.akhyou.Utils.SearchEvent;
import nucleus.presenter.RxPresenter;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func0;
import rx.schedulers.Schedulers;

public class SearchPresenter extends RxPresenter<SearchFragment> {
    private Subscription subscription;
    private SearchProvider searchProvider;
    private String searchTerm;

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        if (searchProvider == null) {
            searchProvider = new AnimeRushSearchProvider();
        }

    }

    @Override
    protected void onTakeView(SearchFragment view) {
        super.onTakeView(view);
        EventBus.getDefault().registerSticky(this);
    }

    @Override
    protected void onDropView() {
        super.onDropView();
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        searchProvider = null;
        subscription.unsubscribe();
    }

    public void onEvent (SearchEvent event) {
        this.searchTerm = event.searchTerm;
        search();
    }

    public void search () {
        if (subscription != null) {
            if (!subscription.isUnsubscribed()) {
                subscription.unsubscribe();
            }
        }
        subscription = Observable.defer(new Func0<Observable<Anime[]>>() {
            @Override
            public Observable<Anime[]> call() {
                return Observable.just(searchProvider.searchFor(searchTerm));
            }
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .compose(this.deliverLatestCache())
                .subscribe(new Subscriber<Anime[]>() {
                    @Override
                    public void onNext(Anime[] animes) {
                        getView().postSuccess();
                        getView().setSearchResults(animes);
                    }

                    @Override
                    public void onCompleted() {
                        getView().setRefreshingFalse();
                        subscription.unsubscribe();
                    }

                    @Override
                    public void onError(Throwable e) {
                        getView().postError();
                        getView().setRefreshingFalse();
                        e.printStackTrace();
                    }
                });
    }

}
