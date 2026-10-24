import 'package:flutter/material.dart';

class BrandMark extends StatelessWidget {
  const BrandMark({super.key, this.size = 32});
  final double size;
  @override
  Widget build(BuildContext context) => Semantics(label: 'Mão na Roda', image: true, child: Container(width: size, height: size, decoration: BoxDecoration(color: Theme.of(context).colorScheme.primary, borderRadius: BorderRadius.circular(size * .3)), child: Icon(Icons.alt_route_rounded, color: Theme.of(context).colorScheme.onPrimary, size: size * .62)));
}
