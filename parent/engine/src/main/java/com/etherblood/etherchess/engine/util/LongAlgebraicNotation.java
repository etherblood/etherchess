package com.etherblood.etherchess.engine.util;

import com.etherblood.etherchess.engine.Move;
import com.etherblood.etherchess.engine.State;

public class LongAlgebraicNotation {


    public static Move parseLanString(State state, String lan) {
        if ("0000".equals(lan)) {
            // TODO: implement nullmove
            throw new UnsupportedOperationException();
        }
        int from = Square.parse(lan.substring(0, 2));
        int to = Square.parse(lan.substring(2, 4));
        if (!state.isWhite) {
            from = Square.mirrorY(from);
            to = Square.mirrorY(to);
        }
        if (lan.length() == 5) {
            int promotion = Piece.fromCharacter(lan.charAt(4));
            int promotionType;
            switch (promotion) {
                case Piece.QUEEN:
                    promotionType = Move.PROMOTION_QUEEN;
                    break;
                case Piece.KNIGHT:
                    promotionType = Move.PROMOTION_KNIGHT;
                    break;
                case Piece.ROOK:
                    promotionType = Move.PROMOTION_ROOK;
                    break;
                case Piece.BISHOP:
                    promotionType = Move.PROMOTION_BISHOP;
                    break;
                default:
                    throw new AssertionError(promotion);
            }
            return Move.promotion(promotionType, from, to);
        }
        int piece = state.getSquarePiece(from);
        switch (piece) {
            case Piece.PAWN:
                if (to == state.enPassantSquare) {
                    return Move.enPassant(from, to);
                }
                if (Math.abs(to - from) == 16) {
                    return Move.pawnDouble(from, to);
                }
                return Move.defaultMove(piece, from, to);
            case Piece.KING:
                if (Math.abs(to - from) == 2) {
                    return new Move(Move.CASTLING, piece, from, to);
                }
                return Move.defaultMove(piece, from, to);
            default:
                return Move.defaultMove(piece, from, to);
        }
    }

    public static String toLanString(boolean isWhite, Move move) {
        int from = move.from();
        int to = move.to();
        if (!isWhite) {
            from = Square.mirrorY(from);
            to = Square.mirrorY(to);
        }
        String s = Square.toString(from) + Square.toString(to);
        switch (move.type()) {
            case Move.PROMOTION_QUEEN:
                s += Piece.toString(Piece.QUEEN);
                break;
            case Move.PROMOTION_ROOK:
                s += Piece.toString(Piece.ROOK);
                break;
            case Move.PROMOTION_BISHOP:
                s += Piece.toString(Piece.BISHOP);
                break;
            case Move.PROMOTION_KNIGHT:
                s += Piece.toString(Piece.KNIGHT);
                break;
        }
        return s;
    }
}
