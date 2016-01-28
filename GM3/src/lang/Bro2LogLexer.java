// Generated from C:\GrassmarlinSVN\branches\gm.all.3.0.0\lang\Bro2Log.g4 by ANTLR 4.1
package lang;

	import java.util.ArrayList;
	import java.util.List;
	import java.util.HashMap;
    import java.util.Optional;
    import java.util.function.Function;
    import java.util.function.Consumer;

import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class Bro2LogLexer extends Lexer {
	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		FIELDS=1, TYPES=2, DIRECTIVE=3, DIR_UNSET=4, DIR_EMPTY=5, IDENT=6, END=7, 
		WS=8;
	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	public static final String[] tokenNames = {
		"<INVALID>",
		"'#fields'", "'#types'", "DIRECTIVE", "'#unset_field'", "'#empty_field'", 
		"IDENT", "END", "WS"
	};
	public static final String[] ruleNames = {
		"FIELDS", "TYPES", "DIRECTIVE", "DIR_UNSET", "DIR_EMPTY", "IDENT", "END", 
		"WS", "TextChar"
	};


	    Optional<Consumer<HashMap<String,String>>> logEmitter = Optional.empty();
	    Function<String,Boolean> filter = Bro2LogLexer::fieldFilter;
		List<String> column_type = new ArrayList<>();
		List<String> column_id = new ArrayList<>();
	    String empty_symbol = "(empty)"; // a typical default
	    String unset_symbol = "-"; // a typical default
	    int column_count = 0;
	    
	    public void onData( Consumer<HashMap<String,String>> cb ) {
	        logEmitter = Optional.of(cb);
	    }
	    
	    static Boolean fieldFilter( String s ) {
	        return true;
	    }
	    
	    public void setFilter(Function<String,Boolean> filter) {
	        this.filter = filter;
	    }
	    
	    void checkBadHeaders() {
	        if(  column_id.isEmpty() ) {
	            throw  new ParseCancellationException("Could not parse #Field or #Types header");
	        } else if( column_id.size() != column_type.size() ) {
	            throw new ParseCancellationException("Malformed column identifiers");
	        }
	    }
	    
	    // l - list to add to
	    // i - index of the column 'column_id.get(i); ...'
	    // content - token content
	    void typeOrSet(HashMap<String,String> map, int i, String content) {
	        if( empty_symbol.equals( content ) ) return; 
	        if( unset_symbol.equals( content ) ) return;
	        String id = column_id.get(i); // should we check bounds or let if fail as "malformed"
	        
	        if( filter.apply( column_id.get(i) ) )
	            map.put( column_id.get(i), content);
	    }
	    


	public Bro2LogLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "Bro2Log.g4"; }

	@Override
	public String[] getTokenNames() { return tokenNames; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	@Override
	public void action(RuleContext _localctx, int ruleIndex, int actionIndex) {
		switch (ruleIndex) {
		case 7: WS_action((RuleContext)_localctx, actionIndex); break;
		}
	}
	private void WS_action(RuleContext _localctx, int actionIndex) {
		switch (actionIndex) {
		case 0: skip();  break;
		}
	}

	public static final String _serializedATN =
		"\3\uacf5\uee8c\u4f5d\u8b0d\u4a45\u78bd\u1b2f\u3378\2\nU\b\1\4\2\t\2\4"+
		"\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\3\2\3\2"+
		"\3\2\3\2\3\2\3\2\3\2\3\2\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\4\3\4\6\4\'\n\4"+
		"\r\4\16\4(\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\6\3\6"+
		"\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\7\6\7F\n\7\r\7\16\7G\3"+
		"\b\5\bK\n\b\3\b\3\b\3\t\3\t\3\t\3\t\3\n\5\nT\n\n\2\13\3\3\1\5\4\1\7\5"+
		"\1\t\6\1\13\7\1\r\b\1\17\t\1\21\n\2\23\2\1\3\2\6\3\2\17\17\3\2\f\f\5\2"+
		"\13\13\17\17\"\"\20\2*+.<AAC_aac|\u00b9\u00b9\u0302\u0371\u2041\u2042"+
		"\u2072\u2191\u2c02\u2ff1\u3003\ud801\uf902\ufdd1\ufdf2\uffffV\2\3\3\2"+
		"\2\2\2\5\3\2\2\2\2\7\3\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2\2\r\3\2\2\2\2\17"+
		"\3\2\2\2\2\21\3\2\2\2\3\25\3\2\2\2\5\35\3\2\2\2\7$\3\2\2\2\t*\3\2\2\2"+
		"\13\67\3\2\2\2\rE\3\2\2\2\17J\3\2\2\2\21N\3\2\2\2\23S\3\2\2\2\25\26\7"+
		"%\2\2\26\27\7h\2\2\27\30\7k\2\2\30\31\7g\2\2\31\32\7n\2\2\32\33\7f\2\2"+
		"\33\34\7u\2\2\34\4\3\2\2\2\35\36\7%\2\2\36\37\7v\2\2\37 \7{\2\2 !\7r\2"+
		"\2!\"\7g\2\2\"#\7u\2\2#\6\3\2\2\2$&\7%\2\2%\'\5\23\n\2&%\3\2\2\2\'(\3"+
		"\2\2\2(&\3\2\2\2()\3\2\2\2)\b\3\2\2\2*+\7%\2\2+,\7w\2\2,-\7p\2\2-.\7u"+
		"\2\2./\7g\2\2/\60\7v\2\2\60\61\7a\2\2\61\62\7h\2\2\62\63\7k\2\2\63\64"+
		"\7g\2\2\64\65\7n\2\2\65\66\7f\2\2\66\n\3\2\2\2\678\7%\2\289\7g\2\29:\7"+
		"o\2\2:;\7r\2\2;<\7v\2\2<=\7{\2\2=>\7a\2\2>?\7h\2\2?@\7k\2\2@A\7g\2\2A"+
		"B\7n\2\2BC\7f\2\2C\f\3\2\2\2DF\5\23\n\2ED\3\2\2\2FG\3\2\2\2GE\3\2\2\2"+
		"GH\3\2\2\2H\16\3\2\2\2IK\t\2\2\2JI\3\2\2\2JK\3\2\2\2KL\3\2\2\2LM\t\3\2"+
		"\2M\20\3\2\2\2NO\t\4\2\2OP\3\2\2\2PQ\b\t\2\2Q\22\3\2\2\2RT\t\5\2\2SR\3"+
		"\2\2\2T\24\3\2\2\2\7\2(GJS";
	public static final ATN _ATN =
		ATNSimulator.deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}