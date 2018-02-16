// Generated from resources/grammar/Calc.g4 by ANTLR 4.5
package util.parser;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class CalcLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.5", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		PLUS=1, MINUS=2, TIMES=3, DIV=4, MOD=5, NUMBER=6, BR_OPEN=7, BR_CLOSE=8, 
		WS=9;
	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	public static final String[] ruleNames = {
		"PLUS", "MINUS", "TIMES", "DIV", "MOD", "NUMBER", "BR_OPEN", "BR_CLOSE", 
		"WS"
	};

	private static final String[] _LITERAL_NAMES = {
		null, null, null, null, null, null, null, "'('", "')'"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, "PLUS", "MINUS", "TIMES", "DIV", "MOD", "NUMBER", "BR_OPEN", "BR_CLOSE", 
		"WS"
	};
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}


	public CalcLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "Calc.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	public static final String _serializedATN =
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\2\13K\b\1\4\2\t\2\4"+
		"\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\3\2\3\2"+
		"\3\2\3\2\3\2\5\2\33\n\2\3\3\3\3\3\3\3\3\3\3\3\3\5\3#\n\3\3\4\3\4\3\4\3"+
		"\4\3\4\3\4\5\4+\n\4\3\5\3\5\3\5\3\5\5\5\61\n\5\3\6\3\6\3\6\3\6\5\6\67"+
		"\n\6\3\7\5\7:\n\7\3\7\6\7=\n\7\r\7\16\7>\3\b\3\b\3\t\3\t\3\n\6\nF\n\n"+
		"\r\n\16\nG\3\n\3\n\2\2\13\3\3\5\4\7\5\t\6\13\7\r\b\17\t\21\n\23\13\3\2"+
		"\4\3\2\62;\5\2\13\f\17\17\"\"R\2\3\3\2\2\2\2\5\3\2\2\2\2\7\3\2\2\2\2\t"+
		"\3\2\2\2\2\13\3\2\2\2\2\r\3\2\2\2\2\17\3\2\2\2\2\21\3\2\2\2\2\23\3\2\2"+
		"\2\3\32\3\2\2\2\5\"\3\2\2\2\7*\3\2\2\2\t\60\3\2\2\2\13\66\3\2\2\2\r9\3"+
		"\2\2\2\17@\3\2\2\2\21B\3\2\2\2\23E\3\2\2\2\25\26\7r\2\2\26\27\7n\2\2\27"+
		"\30\7w\2\2\30\33\7u\2\2\31\33\7-\2\2\32\25\3\2\2\2\32\31\3\2\2\2\33\4"+
		"\3\2\2\2\34\35\7o\2\2\35\36\7k\2\2\36\37\7p\2\2\37 \7w\2\2 #\7u\2\2!#"+
		"\7/\2\2\"\34\3\2\2\2\"!\3\2\2\2#\6\3\2\2\2$%\7v\2\2%&\7k\2\2&\'\7o\2\2"+
		"\'(\7g\2\2(+\7u\2\2)+\7,\2\2*$\3\2\2\2*)\3\2\2\2+\b\3\2\2\2,-\7f\2\2-"+
		".\7k\2\2.\61\7x\2\2/\61\7\61\2\2\60,\3\2\2\2\60/\3\2\2\2\61\n\3\2\2\2"+
		"\62\63\7o\2\2\63\64\7q\2\2\64\67\7f\2\2\65\67\7\'\2\2\66\62\3\2\2\2\66"+
		"\65\3\2\2\2\67\f\3\2\2\28:\7/\2\298\3\2\2\29:\3\2\2\2:<\3\2\2\2;=\t\2"+
		"\2\2<;\3\2\2\2=>\3\2\2\2><\3\2\2\2>?\3\2\2\2?\16\3\2\2\2@A\7*\2\2A\20"+
		"\3\2\2\2BC\7+\2\2C\22\3\2\2\2DF\t\3\2\2ED\3\2\2\2FG\3\2\2\2GE\3\2\2\2"+
		"GH\3\2\2\2HI\3\2\2\2IJ\b\n\2\2J\24\3\2\2\2\13\2\32\"*\60\669>G\3\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}