package is4ape.pm.memoize;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Memoization decorator for BiFunctions
 * 
 * @author Steven
 *
 * @param <X1> type of first function argument
 * @param <X2> type of second function argument
 * @param <Y> type of output
 */
public class MemoizedBiFunction<X1,X2,Y> implements BiFunction<X1,X2,Y>{
	private BiFunction<X1,X2,Y> f;
	private Map<Key,Y> cache;
	
	public MemoizedBiFunction(BiFunction<X1,X2,Y> f){
		this.f = f;
		cache = new HashMap<Key,Y>();
	}

	@Override
	public Y apply(X1 x1, X2 x2) {
		Key key = new Key(x1,x2);
		Y y = cache.get(key);
		if(y == null){
			y = f.apply(x1, x2);
			cache.put(key, y);
		}
		return y;
	}

	class Key{
		final X1 first;
		final X2 second;
		
		Key(X1 first, X2 second){
			this.first = first;
			this.second = second;
		}
		
		public int hashCode(){
			int hash = 23;
			hash = hash * 31 + first.hashCode();
			hash = hash * 31 + second.hashCode();
			return hash;
		}
		
		public boolean equals(Object obj){
			if(obj instanceof MemoizedBiFunction.Key){
				@SuppressWarnings("unchecked")
				Key okey = (Key) obj;
				return first.equals(okey.first) && second.equals(okey.second);
			}
			return false;
		}
	}
	
	public static <X1,X2,Y> MemoizedBiFunction<X1,X2,Y> from(BiFunction<X1,X2,Y> f){
		return new MemoizedBiFunction<X1,X2,Y>(f);
	}

}
