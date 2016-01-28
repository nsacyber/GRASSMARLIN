// Generated from C:\GrassmarlinSVN\branches\gm.all.3.0.0\lang\Bro2Log.g4 by ANTLR 4.1
package lang;

	import java.util.ArrayList;
	import java.util.List;
	import java.util.HashMap;
    import java.util.Optional;
    import java.util.function.Function;
    import java.util.function.Consumer;

import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Map;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class Bro2LogParser extends Parser {
	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		FIELDS=1, TYPES=2, DIRECTIVE=3, DIR_UNSET=4, DIR_EMPTY=5, IDENT=6, END=7, 
		WS=8;
	public static final String[] tokenNames = {
		"<INVALID>", "'#fields'", "'#types'", "DIRECTIVE", "'#unset_field'", "'#empty_field'", 
		"IDENT", "END", "WS"
	};
	public static final int
		RULE_document = 0, RULE_header = 1, RULE_log = 2, RULE_footer = 3, RULE_fields = 4, 
		RULE_types = 5, RULE_directive = 6, RULE_normalField = 7, RULE_unsetField = 8, 
		RULE_emptyField = 9, RULE_entry = 10, RULE_typeList = 11, RULE_fieldList = 12;
	public static final String[] ruleNames = {
		"document", "header", "log", "footer", "fields", "types", "directive", 
		"normalField", "unsetField", "emptyField", "entry", "typeList", "fieldList"
	};

	@Override
	public String getGrammarFileName() { return "Bro2Log.g4"; }

	@Override
	public String[] getTokenNames() { return tokenNames; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public ATN getATN() { return _ATN; }


	    Optional<Consumer<Map<String,String>>> logEmitter = Optional.empty();
	    Function<String,Boolean> filter = Bro2LogLexer::fieldFilter;
		List<String> column_type = new ArrayList<>();
		List<String> column_id = new ArrayList<>();
	    String empty_symbol = "(empty)"; // a typical default
	    String unset_symbol = "-"; // a typical default
	    int column_count = 0;
	    
	    public void onData( Consumer<Map<String,String>> cb ) {
	        logEmitter = Optional.of(cb);
	    }
	    
	    static Boolean fieldFilter( String s ) {
	        return true;
	    }
	    
        /**
         * set a callback which will determine if a string is acceptable to include in the extracted data
         * @param filter Callback to be run on each content String extracted from the raw file.
         */
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
	        if( empty_symbol.equals( content ) ) {
                return;
            } 
	        if( unset_symbol.equals( content ) ) {
                return;
            }
	        String id = column_id.get(i); // should we check bounds or let if fail as "malformed"
	        
	        if( filter.apply( id ) )
	            map.put( id, content);
	    }
	    

	public Bro2LogParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}
	public static class DocumentContext extends ParserRuleContext {
		public List<FooterContext> footer() {
			return getRuleContexts(FooterContext.class);
		}
		public TerminalNode EOF() { return getToken(Bro2LogParser.EOF, 0); }
		public List<LogContext> log() {
			return getRuleContexts(LogContext.class);
		}
		public HeaderContext header() {
			return getRuleContext(HeaderContext.class,0);
		}
		public FooterContext footer(int i) {
			return getRuleContext(FooterContext.class,i);
		}
		public LogContext log(int i) {
			return getRuleContext(LogContext.class,i);
		}
		public DocumentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_document; }
	}

	public final DocumentContext document() throws RecognitionException {
		DocumentContext _localctx = new DocumentContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_document);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(26); header();
			setState(30);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,0,_ctx);
			while ( _alt!=2 && _alt!=-1 ) {
				if ( _alt==1 ) {
					{
					{
					setState(27); log();
					}
					} 
				}
				setState(32);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,0,_ctx);
			}
			setState(36);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << DIRECTIVE) | (1L << DIR_UNSET) | (1L << DIR_EMPTY) | (1L << END))) != 0)) {
				{
				{
				setState(33); footer();
				}
				}
				setState(38);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(39); match(EOF);
			}

			    checkBadHeaders();

		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class HeaderContext extends ParserRuleContext {
		public DirectiveContext directive(int i) {
			return getRuleContext(DirectiveContext.class,i);
		}
		public TerminalNode END(int i) {
			return getToken(Bro2LogParser.END, i);
		}
		public List<DirectiveContext> directive() {
			return getRuleContexts(DirectiveContext.class);
		}
		public List<TypesContext> types() {
			return getRuleContexts(TypesContext.class);
		}
		public List<TerminalNode> END() { return getTokens(Bro2LogParser.END); }
		public List<FieldsContext> fields() {
			return getRuleContexts(FieldsContext.class);
		}
		public FieldsContext fields(int i) {
			return getRuleContext(FieldsContext.class,i);
		}
		public TypesContext types(int i) {
			return getRuleContext(TypesContext.class,i);
		}
		public HeaderContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_header; }
	}

	public final HeaderContext header() throws RecognitionException {
		HeaderContext _localctx = new HeaderContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_header);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(52); 
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,5,_ctx);
			do {
				switch (_alt) {
				case 1:
					{
					{
					setState(48);
					_errHandler.sync(this);
					_la = _input.LA(1);
					while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << FIELDS) | (1L << TYPES) | (1L << DIRECTIVE) | (1L << DIR_UNSET) | (1L << DIR_EMPTY))) != 0)) {
						{
						setState(46);
						switch (_input.LA(1)) {
						case FIELDS:
						case TYPES:
							{
							setState(43);
							switch (_input.LA(1)) {
							case FIELDS:
								{
								setState(41); fields();
								}
								break;
							case TYPES:
								{
								setState(42); types();
								}
								break;
							default:
								throw new NoViableAltException(this);
							}
							}
							break;
						case DIRECTIVE:
						case DIR_UNSET:
						case DIR_EMPTY:
							{
							setState(45); directive();
							}
							break;
						default:
							throw new NoViableAltException(this);
						}
						}
						setState(50);
						_errHandler.sync(this);
						_la = _input.LA(1);
					}
					setState(51); match(END);
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(54); 
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,5,_ctx);
			} while ( _alt!=2 && _alt!=-1 );
			}

			    checkBadHeaders();

		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class LogContext extends ParserRuleContext {
		public EntryContext ent;
		public EntryContext entry(int i) {
			return getRuleContext(EntryContext.class,i);
		}
		public TerminalNode END() { return getToken(Bro2LogParser.END, 0); }
		public List<EntryContext> entry() {
			return getRuleContexts(EntryContext.class);
		}
		public LogContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_log; }
	}

	public final LogContext log() throws RecognitionException {
		LogContext _localctx = new LogContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_log);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(61);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==IDENT) {
				{
				{
				setState(56); ((LogContext)_localctx).ent = entry();

				        logEmitter.ifPresent(cb->cb.accept(((LogContext)_localctx).ent.ret));
				    
				}
				}
				setState(63);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(64); match(END);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FooterContext extends ParserRuleContext {
		public DirectiveContext directive(int i) {
			return getRuleContext(DirectiveContext.class,i);
		}
		public List<DirectiveContext> directive() {
			return getRuleContexts(DirectiveContext.class);
		}
		public TerminalNode END() { return getToken(Bro2LogParser.END, 0); }
		public FooterContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_footer; }
	}

	public final FooterContext footer() throws RecognitionException {
		FooterContext _localctx = new FooterContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_footer);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(69);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << DIRECTIVE) | (1L << DIR_UNSET) | (1L << DIR_EMPTY))) != 0)) {
				{
				{
				setState(66); directive();
				}
				}
				setState(71);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(72); match(END);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FieldsContext extends ParserRuleContext {
		public FieldListContext fieldList() {
			return getRuleContext(FieldListContext.class,0);
		}
		public TerminalNode FIELDS() { return getToken(Bro2LogParser.FIELDS, 0); }
		public FieldsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fields; }
	}

	public final FieldsContext fields() throws RecognitionException {
		FieldsContext _localctx = new FieldsContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_fields);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(74); match(FIELDS);
			setState(75); fieldList();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class TypesContext extends ParserRuleContext {
		public TypeListContext typeList() {
			return getRuleContext(TypeListContext.class,0);
		}
		public TerminalNode TYPES() { return getToken(Bro2LogParser.TYPES, 0); }
		public TypesContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_types; }
	}

	public final TypesContext types() throws RecognitionException {
		TypesContext _localctx = new TypesContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_types);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(77); match(TYPES);
			setState(78); typeList();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class DirectiveContext extends ParserRuleContext {
		public EmptyFieldContext emptyField() {
			return getRuleContext(EmptyFieldContext.class,0);
		}
		public NormalFieldContext normalField() {
			return getRuleContext(NormalFieldContext.class,0);
		}
		public UnsetFieldContext unsetField() {
			return getRuleContext(UnsetFieldContext.class,0);
		}
		public DirectiveContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_directive; }
	}

	public final DirectiveContext directive() throws RecognitionException {
		DirectiveContext _localctx = new DirectiveContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_directive);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(83);
			switch (_input.LA(1)) {
			case DIR_EMPTY:
				{
				setState(80); emptyField();
				}
				break;
			case DIR_UNSET:
				{
				setState(81); unsetField();
				}
				break;
			case DIRECTIVE:
				{
				setState(82); normalField();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class NormalFieldContext extends ParserRuleContext {
		public TerminalNode DIRECTIVE() { return getToken(Bro2LogParser.DIRECTIVE, 0); }
		public TerminalNode IDENT() { return getToken(Bro2LogParser.IDENT, 0); }
		public NormalFieldContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_normalField; }
	}

	public final NormalFieldContext normalField() throws RecognitionException {
		NormalFieldContext _localctx = new NormalFieldContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_normalField);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(85); match(DIRECTIVE);
			setState(86); match(IDENT);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class UnsetFieldContext extends ParserRuleContext {
		public Token ident;
		public TerminalNode DIR_UNSET() { return getToken(Bro2LogParser.DIR_UNSET, 0); }
		public TerminalNode IDENT() { return getToken(Bro2LogParser.IDENT, 0); }
		public UnsetFieldContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_unsetField; }
	}

	public final UnsetFieldContext unsetField() throws RecognitionException {
		UnsetFieldContext _localctx = new UnsetFieldContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_unsetField);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(88); match(DIR_UNSET);
			setState(89); ((UnsetFieldContext)_localctx).ident = match(IDENT);
			 unset_symbol=((UnsetFieldContext)_localctx).ident.getText(); 
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class EmptyFieldContext extends ParserRuleContext {
		public Token ident;
		public TerminalNode DIR_EMPTY() { return getToken(Bro2LogParser.DIR_EMPTY, 0); }
		public TerminalNode IDENT() { return getToken(Bro2LogParser.IDENT, 0); }
		public EmptyFieldContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_emptyField; }
	}

	public final EmptyFieldContext emptyField() throws RecognitionException {
		EmptyFieldContext _localctx = new EmptyFieldContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_emptyField);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(92); match(DIR_EMPTY);
			setState(93); ((EmptyFieldContext)_localctx).ident = match(IDENT);
			 empty_symbol=((EmptyFieldContext)_localctx).ident.getText(); 
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class EntryContext extends ParserRuleContext {
		public HashMap<String,String> ret;
		public Token ident;
		public TerminalNode IDENT(int i) {
			return getToken(Bro2LogParser.IDENT, i);
		}
		public List<TerminalNode> IDENT() { return getTokens(Bro2LogParser.IDENT); }
		public EntryContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_entry; }
	}

	public final EntryContext entry() throws RecognitionException {
		EntryContext _localctx = new EntryContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_entry);

		    HashMap<String,String> ret = new HashMap<>();
		    int index = 0;

		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(98); 
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,9,_ctx);
			do {
				switch (_alt) {
				case 1:
					{
					{
					setState(96); ((EntryContext)_localctx).ident = match(IDENT);

					        typeOrSet( ret, index++, ((EntryContext)_localctx).ident.getText() );
					    
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(100); 
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,9,_ctx);
			} while ( _alt!=2 && _alt!=-1 );
			}

			    ((EntryContext)_localctx).ret =  ret;

		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class TypeListContext extends ParserRuleContext {
		public Token ident;
		public TerminalNode IDENT(int i) {
			return getToken(Bro2LogParser.IDENT, i);
		}
		public List<TerminalNode> IDENT() { return getTokens(Bro2LogParser.IDENT); }
		public TypeListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typeList; }
	}

	public final TypeListContext typeList() throws RecognitionException {
		TypeListContext _localctx = new TypeListContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_typeList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(104); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(102); ((TypeListContext)_localctx).ident = match(IDENT);

				        column_type.add(((TypeListContext)_localctx).ident.getText());
				    
				}
				}
				setState(106); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==IDENT );
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FieldListContext extends ParserRuleContext {
		public Token ident;
		public TerminalNode IDENT(int i) {
			return getToken(Bro2LogParser.IDENT, i);
		}
		public List<TerminalNode> IDENT() { return getTokens(Bro2LogParser.IDENT); }
		public FieldListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fieldList; }
	}

	public final FieldListContext fieldList() throws RecognitionException {
		FieldListContext _localctx = new FieldListContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_fieldList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(110); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(108); ((FieldListContext)_localctx).ident = match(IDENT);

				        column_id.add(((FieldListContext)_localctx).ident.getText());
				    
				}
				}
				setState(112); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==IDENT );
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static final String _serializedATN =
		"\3\uacf5\uee8c\u4f5d\u8b0d\u4a45\u78bd\u1b2f\u3378\3\nu\4\2\t\2\4\3\t"+
		"\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13\t\13\4"+
		"\f\t\f\4\r\t\r\4\16\t\16\3\2\3\2\7\2\37\n\2\f\2\16\2\"\13\2\3\2\7\2%\n"+
		"\2\f\2\16\2(\13\2\3\2\3\2\3\3\3\3\5\3.\n\3\3\3\7\3\61\n\3\f\3\16\3\64"+
		"\13\3\3\3\6\3\67\n\3\r\3\16\38\3\4\3\4\3\4\7\4>\n\4\f\4\16\4A\13\4\3\4"+
		"\3\4\3\5\7\5F\n\5\f\5\16\5I\13\5\3\5\3\5\3\6\3\6\3\6\3\7\3\7\3\7\3\b\3"+
		"\b\3\b\5\bV\n\b\3\t\3\t\3\t\3\n\3\n\3\n\3\n\3\13\3\13\3\13\3\13\3\f\3"+
		"\f\6\fe\n\f\r\f\16\ff\3\r\3\r\6\rk\n\r\r\r\16\rl\3\16\3\16\6\16q\n\16"+
		"\r\16\16\16r\3\16\2\17\2\4\6\b\n\f\16\20\22\24\26\30\32\2\2t\2\34\3\2"+
		"\2\2\4\66\3\2\2\2\6?\3\2\2\2\bG\3\2\2\2\nL\3\2\2\2\fO\3\2\2\2\16U\3\2"+
		"\2\2\20W\3\2\2\2\22Z\3\2\2\2\24^\3\2\2\2\26d\3\2\2\2\30j\3\2\2\2\32p\3"+
		"\2\2\2\34 \5\4\3\2\35\37\5\6\4\2\36\35\3\2\2\2\37\"\3\2\2\2 \36\3\2\2"+
		"\2 !\3\2\2\2!&\3\2\2\2\" \3\2\2\2#%\5\b\5\2$#\3\2\2\2%(\3\2\2\2&$\3\2"+
		"\2\2&\'\3\2\2\2\')\3\2\2\2(&\3\2\2\2)*\7\2\2\3*\3\3\2\2\2+.\5\n\6\2,."+
		"\5\f\7\2-+\3\2\2\2-,\3\2\2\2.\61\3\2\2\2/\61\5\16\b\2\60-\3\2\2\2\60/"+
		"\3\2\2\2\61\64\3\2\2\2\62\60\3\2\2\2\62\63\3\2\2\2\63\65\3\2\2\2\64\62"+
		"\3\2\2\2\65\67\7\t\2\2\66\62\3\2\2\2\678\3\2\2\28\66\3\2\2\289\3\2\2\2"+
		"9\5\3\2\2\2:;\5\26\f\2;<\b\4\1\2<>\3\2\2\2=:\3\2\2\2>A\3\2\2\2?=\3\2\2"+
		"\2?@\3\2\2\2@B\3\2\2\2A?\3\2\2\2BC\7\t\2\2C\7\3\2\2\2DF\5\16\b\2ED\3\2"+
		"\2\2FI\3\2\2\2GE\3\2\2\2GH\3\2\2\2HJ\3\2\2\2IG\3\2\2\2JK\7\t\2\2K\t\3"+
		"\2\2\2LM\7\3\2\2MN\5\32\16\2N\13\3\2\2\2OP\7\4\2\2PQ\5\30\r\2Q\r\3\2\2"+
		"\2RV\5\24\13\2SV\5\22\n\2TV\5\20\t\2UR\3\2\2\2US\3\2\2\2UT\3\2\2\2V\17"+
		"\3\2\2\2WX\7\5\2\2XY\7\b\2\2Y\21\3\2\2\2Z[\7\6\2\2[\\\7\b\2\2\\]\b\n\1"+
		"\2]\23\3\2\2\2^_\7\7\2\2_`\7\b\2\2`a\b\13\1\2a\25\3\2\2\2bc\7\b\2\2ce"+
		"\b\f\1\2db\3\2\2\2ef\3\2\2\2fd\3\2\2\2fg\3\2\2\2g\27\3\2\2\2hi\7\b\2\2"+
		"ik\b\r\1\2jh\3\2\2\2kl\3\2\2\2lj\3\2\2\2lm\3\2\2\2m\31\3\2\2\2no\7\b\2"+
		"\2oq\b\16\1\2pn\3\2\2\2qr\3\2\2\2rp\3\2\2\2rs\3\2\2\2s\33\3\2\2\2\16 "+
		"&-\60\628?GUflr";
	public static final ATN _ATN =
		ATNSimulator.deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}