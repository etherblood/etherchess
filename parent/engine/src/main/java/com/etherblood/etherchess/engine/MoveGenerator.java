package com.etherblood.etherchess.engine;

import com.etherblood.etherchess.engine.util.Castling;
import com.etherblood.etherchess.engine.util.Direction;
import com.etherblood.etherchess.engine.util.Piece;
import com.etherblood.etherchess.engine.util.PieceSquareSet;
import com.etherblood.etherchess.engine.util.Square;
import com.etherblood.etherchess.engine.util.SquareSet;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class MoveGenerator {

    public static final Move LARGE_CASTLING = new Move(Move.CASTLING, Piece.KING, Square.E1, Square.C1);
    public static final Move SMALL_CASTLING = new Move(Move.CASTLING, Piece.KING, Square.E1, Square.G1);

    public void generatePseudoLegalMoves(State state, Consumer<Move> out) {
        long pinnedMask = 0;
        long pushMask = ~0;
        long captureMask = ~0;
        pseudoLegalKingMoves(state, out);
        pawnMoves(state, state.pawns() & state.own() & ~pinnedMask, pushMask, captureMask, out);
        knightMoves(state, state.knights() & state.own() & ~pinnedMask, pushMask, captureMask, out);
        bishopMoves(state, state.bishops() & state.own() & ~pinnedMask, pushMask, captureMask, out);
        rookMoves(state, state.rooks() & state.own() & ~pinnedMask, pushMask, captureMask, out);
        queenMoves(state, state.queens() & state.own() & ~pinnedMask, pushMask, captureMask, out);
    }

    public List<Move> generateLegalMoves(State state) {
        List<Move> moves = new ArrayList<>();
        generateLegalMoves(state, moves::add);
        return moves;
    }

    public void generateLegalMoves(State state, Consumer<Move> out) {
        assert state.assertValid();
        long kingDangerSquares = kingDangerSquares(state);

        legalKingMoves(state, kingDangerSquares, out);
        long captureMask = state.opp();
        long pushMask = ~0;
        long ownKings = state.kings() & state.own();
        if ((ownKings & kingDangerSquares) != 0) {
            long checkers = findOpponentCheckers(state);
            int checkerCount = SquareSet.count(checkers);
            if (checkerCount > 1) {
                // more than 1 opponent piece is giving check, only king moves can evade it
                return;
            }
            assert checkerCount == 1;
            captureMask = checkers;
            int checkerSquare = Square.firstOf(checkers);
            if ((checkers & (PieceSquareSet.kingMoves(checkerSquare) | PieceSquareSet.knightMoves(checkerSquare))) != 0) {
                // this if is not required but is cheaper than a full move gen
                long sourceMask = findOwnAttackers(state, checkerSquare);
                pushMask = 0;
                generateDefaultMoves(Piece.QUEEN, sourceMask & state.queens(), checkerSquare, out);
                generateDefaultMoves(Piece.ROOK, sourceMask & state.rooks(), checkerSquare, out);
                generateDefaultMoves(Piece.BISHOP, sourceMask & state.bishops(), checkerSquare, out);
                generateDefaultMoves(Piece.KNIGHT, sourceMask & state.knights(), checkerSquare, out);
                generateDefaultMoves(Piece.KING, sourceMask & state.kings(), checkerSquare, out);
                pawnMoves(state, sourceMask & state.pawns(), pushMask, captureMask, out);
                return;
            }
            int kingSquare = Square.firstOf(ownKings);
            pushMask = PieceSquareSet.raySquaresBetween(kingSquare, checkerSquare);
        }

        long pinnedMask = 0;
        long oppRookLikes = (state.rooks() | state.queens()) & state.opp();
        pinnedMask |= handlePinnedMoves(state, Direction.NORTH, oppRookLikes, pushMask, captureMask, out);
        pinnedMask |= handlePinnedMoves(state, Direction.EAST, oppRookLikes, pushMask, captureMask, out);
        pinnedMask |= handlePinnedMoves(state, Direction.SOUTH, oppRookLikes, pushMask, captureMask, out);
        pinnedMask |= handlePinnedMoves(state, Direction.WEST, oppRookLikes, pushMask, captureMask, out);

        long oppBishopLikes = (state.bishops() | state.queens()) & state.opp();
        pinnedMask |= handlePinnedMoves(state, Direction.NORTH_EAST, oppBishopLikes, pushMask, captureMask, out);
        pinnedMask |= handlePinnedMoves(state, Direction.SOUTH_EAST, oppBishopLikes, pushMask, captureMask, out);
        pinnedMask |= handlePinnedMoves(state, Direction.SOUTH_WEST, oppBishopLikes, pushMask, captureMask, out);
        pinnedMask |= handlePinnedMoves(state, Direction.NORTH_WEST, oppBishopLikes, pushMask, captureMask, out);

        long sourceMask = state.own();
        pawnMoves(state, sourceMask & state.pawns() & ~pinnedMask, pushMask, captureMask, out);
        knightMoves(state, sourceMask & state.knights() & ~pinnedMask, pushMask, captureMask, out);
        bishopMoves(state, sourceMask & state.bishops() & ~pinnedMask, pushMask, captureMask, out);
        rookMoves(state, sourceMask & state.rooks() & ~pinnedMask, pushMask, captureMask, out);
        queenMoves(state, sourceMask & state.queens() & ~pinnedMask, pushMask, captureMask, out);
    }

    private long kingDangerSquares(State state) {
        long ownKings = state.own() & state.kings();
        int ownKingSquare = Square.firstOf(ownKings);
        long occupiedOwnKingExcluded = state.occupied() ^ ownKings;
        long result = 0;
        long pawns = state.pawns() & state.opp();
        result |= (pawns >>> 7) & ~SquareSet.FILE_A;
        result |= (pawns >>> 9) & ~SquareSet.FILE_H;
        long rookLikes = (state.rooks() | state.queens()) & state.opp() & PieceSquareSet.kingDangerRooksMask(ownKingSquare);
        while (rookLikes != 0) {
            int from = Square.firstOf(rookLikes);
            result |= PieceSquareSet.rookRays(from, occupiedOwnKingExcluded);
            rookLikes = SquareSet.clearFirst(rookLikes);
        }
        long bishopLikes = (state.bishops() | state.queens()) & state.opp() & PieceSquareSet.kingDangerBishopsMask(ownKingSquare);
        while (bishopLikes != 0) {
            int from = Square.firstOf(bishopLikes);
            result |= PieceSquareSet.bishopRays(from, occupiedOwnKingExcluded);
            bishopLikes = SquareSet.clearFirst(bishopLikes);
        }
        long knights = state.knights() & state.opp() & PieceSquareSet.kingDangerKnightsMask(ownKingSquare);
        while (knights != 0) {
            int from = Square.firstOf(knights);
            result |= PieceSquareSet.knightMoves(from);
            knights = SquareSet.clearFirst(knights);
        }
        long kings = state.kings() & state.opp();
        int opponentKingSquare = Square.firstOf(kings);
        result |= PieceSquareSet.kingMoves(opponentKingSquare);
        return result;
    }

    public long findOwnCheckers(State state) {
        return findOwnAttackers(state, Square.firstOf(state.kings() & state.opp()));
    }

    public long findOpponentCheckers(State state) {
        return findOpponentAttackers(state, Square.firstOf(state.kings() & state.own()));
    }

    public long findOwnAttackers(State state, int target) {
        long targetSquares = SquareSet.of(target);
        long pawnsMask = ((targetSquares >>> 7) & ~SquareSet.FILE_A) | ((targetSquares >>> 9) & ~SquareSet.FILE_H);
        return findAttackers(state, target, pawnsMask, state.own());
    }

    public long findOpponentAttackers(State state, int target) {
        long targetSquares = SquareSet.of(target);
        long pawnsMask = ((targetSquares << 7) & ~SquareSet.FILE_H) | ((targetSquares << 9) & ~SquareSet.FILE_A);
        return findAttackers(state, target, pawnsMask, state.opp());
    }

    private long findAttackers(State state, int target, long pawnsMask, long attackerMask) {
        long attackers = 0;
        attackers |= PieceSquareSet.knightMoves(target) & attackerMask & state.knights();
        attackers |= pawnsMask & attackerMask & state.pawns();
        attackers |= PieceSquareSet.rookRays(target, state.occupied()) & attackerMask & (state.rooks() | state.queens());
        attackers |= PieceSquareSet.bishopRays(target, state.occupied()) & attackerMask & (state.bishops() | state.queens());
        return attackers;
    }

    private long handlePinnedMoves(State state, int direction, long opponentAttackersMask, long pushMask, long captureMask, Consumer<Move> out) {
        long pinnedSet = 0;
        long ownKings = state.kings() & state.own();
        int kingSquare = Square.firstOf(ownKings);
        long ray = SquareSet.simpleDirectionRay(direction, kingSquare);
        if ((state.own() & ray) != 0 && (ray & opponentAttackersMask) != 0) {
            // check above is not required, but it provides a cheap early exit condition
            long pinRay = PieceSquareSet.directionRay(direction, kingSquare, state.opp());
            long pinned = pinRay & state.own();
            if (SquareSet.count(pinned) == 1) {
                int from = Square.firstOf(pinned);
                if ((pinRay & opponentAttackersMask) != 0) {
                    if ((pinned & state.rooks()) != 0 && !Direction.isDiagonal(direction)) {
                        long slides = pinRay ^ pinned;
                        generateDefaultMoves(Piece.ROOK, from, slides & ~state.own() & (pushMask | captureMask), out);
                    } else if ((pinned & state.queens()) != 0) {
                        long slides = pinRay ^ pinned;
                        generateDefaultMoves(Piece.QUEEN, from, slides & ~state.own() & (pushMask | captureMask), out);
                    } else if ((pinned & state.bishops()) != 0 && Direction.isDiagonal(direction)) {
                        long slides = pinRay ^ pinned;
                        generateDefaultMoves(Piece.BISHOP, from, slides & ~state.own() & (pushMask | captureMask), out);
                    } else if ((pinned & state.pawns()) != 0) {
                        pawnMoves(state, pinned, pushMask & pinRay, captureMask & pinRay, out);
                    } else {
                        // pinned knights can never move
                        // kings can not be pinned
                    }

                    pinnedSet |= pinned;
                }
            }
        }
        return pinnedSet;
    }

    private void queenMoves(State state, long ownQueens, long pushMask, long captureMask, Consumer<Move> out) {
        while (ownQueens != 0) {
            int from = Square.firstOf(ownQueens);

            long occupied = state.own() | state.opp();
            long queenSlides = PieceSquareSet.queenRays(from, occupied);
            generateDefaultMoves(Piece.QUEEN, from, queenSlides & ~state.own() & (pushMask | captureMask), out);

            ownQueens = SquareSet.clearFirst(ownQueens);
        }
    }

    private void rookMoves(State state, long ownRooks, long pushMask, long captureMask, Consumer<Move> out) {
        while (ownRooks != 0) {
            int from = Square.firstOf(ownRooks);

            long occupied = state.own() | state.opp();
            long rookSlides = PieceSquareSet.rookRays(from, occupied);
            generateDefaultMoves(Piece.ROOK, from, rookSlides & ~state.own() & (pushMask | captureMask), out);

            ownRooks = SquareSet.clearFirst(ownRooks);
        }
    }

    private void bishopMoves(State state, long ownBishops, long pushMask, long captureMask, Consumer<Move> out) {
        while (ownBishops != 0) {
            int from = Square.firstOf(ownBishops);

            long occupied = state.own() | state.opp();
            long bishopSlides = PieceSquareSet.bishopRays(from, occupied);
            generateDefaultMoves(Piece.BISHOP, from, bishopSlides & ~state.own() & (pushMask | captureMask), out);

            ownBishops = SquareSet.clearFirst(ownBishops);
        }
    }

    private void knightMoves(State state, long ownKnights, long pushMask, long captureMask, Consumer<Move> out) {
        while (ownKnights != 0) {
            int from = Square.firstOf(ownKnights);

            generateDefaultMoves(Piece.KNIGHT, from, PieceSquareSet.knightMoves(from) & ~state.own() & (pushMask | captureMask), out);

            ownKnights = SquareSet.clearFirst(ownKnights);
        }
    }

    private void pseudoLegalKingMoves(State state, Consumer<Move> out) {
        long ownKings = state.kings() & state.own();
        int from = Square.firstOf(ownKings);
        generateDefaultMoves(Piece.KING, from, PieceSquareSet.kingMoves(from) & ~state.own(), out);
        if ((state.availableCastlings & Castling.A1) != 0) {
            if (((SquareSet.B1 | SquareSet.C1 | SquareSet.D1) & state.occupied()) == 0) {
                if (((SquareSet.C1 | SquareSet.D1 | SquareSet.E1) & kingDangerSquares(state)) == 0) {
                    out.accept(LARGE_CASTLING);
                }
            }
        }
        if ((state.availableCastlings & Castling.H1) != 0) {
            if (((SquareSet.F1 | SquareSet.G1) & state.occupied()) == 0) {
                if (((SquareSet.E1 | SquareSet.F1 | SquareSet.G1) & kingDangerSquares(state)) == 0) {
                    out.accept(SMALL_CASTLING);
                }
            }
        }
    }

    private void legalKingMoves(State state, long kingDangerSquares, Consumer<Move> out) {
        long ownKings = state.kings() & state.own();
        int from = Square.firstOf(ownKings);
        generateDefaultMoves(Piece.KING, from, PieceSquareSet.kingMoves(from) & ~(state.own() | kingDangerSquares), out);
        if ((ownKings & kingDangerSquares) == 0) {
            if ((state.availableCastlings & Castling.A1) != 0) {
                if (((SquareSet.B1 | SquareSet.C1 | SquareSet.D1) & state.occupied()) == 0) {
                    if (((SquareSet.C1 | SquareSet.D1 | SquareSet.E1) & kingDangerSquares) == 0) {
                        out.accept(LARGE_CASTLING);
                    }
                }
            }
            if ((state.availableCastlings & Castling.H1) != 0) {
                if (((SquareSet.F1 | SquareSet.G1) & state.occupied()) == 0) {
                    if (((SquareSet.E1 | SquareSet.F1 | SquareSet.G1) & kingDangerSquares) == 0) {
                        out.accept(SMALL_CASTLING);
                    }
                }
            }
        }
    }

    private void generateDefaultMoves(int piece, int from, long toSquareSet, Consumer<Move> out) {
        while (toSquareSet != 0) {
            int to = Square.firstOf(toSquareSet);
            out.accept(Move.defaultMove(piece, from, to));

            toSquareSet = SquareSet.clearFirst(toSquareSet);
        }
    }

    private void generateDefaultMoves(int piece, long fromSquareSet, int to, Consumer<Move> out) {
        while (fromSquareSet != 0) {
            int from = Square.firstOf(fromSquareSet);
            out.accept(Move.defaultMove(piece, from, to));

            fromSquareSet = SquareSet.clearFirst(fromSquareSet);
        }
    }

    private void pawnMoves(State state, long ownPawns, long pushMask, long captureMask, Consumer<Move> out) {
        long moves = (ownPawns << 8) & ~state.occupied();
        long doubles = (moves << 8) & ~state.occupied() & SquareSet.RANK_4;
        moves &= pushMask;
        while (moves != 0) {
            int to = Square.firstOf(moves);
            int from = to - 8;
            if (to >= Square.A8) {
                out.accept(Move.promotion(Move.PROMOTION_QUEEN, from, to));
                out.accept(Move.promotion(Move.PROMOTION_KNIGHT, from, to));
                out.accept(Move.promotion(Move.PROMOTION_ROOK, from, to));
                out.accept(Move.promotion(Move.PROMOTION_BISHOP, from, to));
            } else {
                out.accept(Move.pawnMove(from, to));
            }

            moves = SquareSet.clearFirst(moves);
        }
        doubles &= pushMask;
        while (doubles != 0) {
            int to = Square.firstOf(doubles);
            int from = to - 16;
            out.accept(Move.pawnDouble(from, to));

            doubles = SquareSet.clearFirst(doubles);
        }

        if (state.enPassantSquare != 0) {
            long pawnMask = (SquareSet.of(state.enPassantSquare - 9) | SquareSet.of(state.enPassantSquare - 7)) & SquareSet.RANK_5;
            long pawns = ownPawns & pawnMask;
            int kingSquare = Square.firstOf(state.kings() & state.own());
            while (pawns != 0) {
                long pawn = SquareSet.firstOf(pawns);
                pawns ^= pawn;
                long occ = state.occupied() ^ (pawn | SquareSet.of(state.enPassantSquare) | SquareSet.of(state.enPassantSquare - 8));
                if ((PieceSquareSet.rookRays(kingSquare, occ) & (state.rooks() | state.queens()) & state.opp()) != 0
                        || (PieceSquareSet.bishopRays(kingSquare, occ) & (state.bishops() | state.queens()) & state.opp()) != 0) {
                    // en passant is pinned and can't be used
                    continue;
                }
                out.accept(Move.enPassant(Square.firstOf(pawn), state.enPassantSquare));
            }
        }

        long leftAttacks = (ownPawns << 7) & ~SquareSet.FILE_H & state.opp() & captureMask;
        while (leftAttacks != 0) {
            int to = Square.firstOf(leftAttacks);
            int from = to - 7;
            if (to >= Square.A8) {
                out.accept(Move.promotion(Move.PROMOTION_QUEEN, from, to));
                out.accept(Move.promotion(Move.PROMOTION_KNIGHT, from, to));
                out.accept(Move.promotion(Move.PROMOTION_ROOK, from, to));
                out.accept(Move.promotion(Move.PROMOTION_BISHOP, from, to));
            } else {
                out.accept(Move.pawnMove(from, to));
            }

            leftAttacks = SquareSet.clearFirst(leftAttacks);
        }
        long rightAttacks = (ownPawns << 9) & ~SquareSet.FILE_A & state.opp() & captureMask;
        while (rightAttacks != 0) {
            int to = Square.firstOf(rightAttacks);
            int from = to - 9;
            if (to >= Square.A8) {
                out.accept(Move.promotion(Move.PROMOTION_QUEEN, from, to));
                out.accept(Move.promotion(Move.PROMOTION_KNIGHT, from, to));
                out.accept(Move.promotion(Move.PROMOTION_ROOK, from, to));
                out.accept(Move.promotion(Move.PROMOTION_BISHOP, from, to));
            } else {
                out.accept(Move.pawnMove(from, to));
            }

            rightAttacks = SquareSet.clearFirst(rightAttacks);
        }
    }
}
